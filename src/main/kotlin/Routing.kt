package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.authentication.LoginRequest
import com.example.authentication.UNAUTHORISED
import com.example.image.ImageFormatter
import com.example.image.ImageFormattingRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.toByteArray
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.minutes



const val issuer = "http://0.0.0.0:8080/"
const val audience = "http://0.0.0.0:8080/hello"
fun Application.configureRouting(controller: AppController) {

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = controller.login(request.username, request.password)
            if (response) {
                val token = generateToken(request.username)

                call.response.headers.append("jwt-token", token)
                call.respond("valid")


            } else {
                call.respond(HttpStatusCode.Unauthorized, UNAUTHORISED)
            }
        }
        post("/sign-up") {
            val loginRequest = call.receive<LoginRequest>()
            val result = controller.signUp(loginRequest.username, loginRequest.password)
            if (result) {
                call.respondText("valid")
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
            call.respondText("valid")
        }

        authenticate("auth-jwt") {
            route("/image") {
                post {
                    val principal = call.principal<JWTPrincipal>()
                    val username = principal!!.payload.getClaim("username").asString()
                    val param = call.receiveMultipart()

                    param.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val result = controller.uploadImage(
                                    part.provider().toByteArray(),
                                    username,
                                    part.originalFileName ?: "$username-file",
                                )
                                //todo remove the returning of the image when uploading to backend
                                call.response.headers.append(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.Json.toString()
                                )
                                call.respond(HttpStatusCode.OK, result.getOrThrow())
                            }

                            is PartData.FormItem -> {
                                //todo handle case
                            }

                            else -> {
                                //todo handle case
                            }
                        }
                    }
                }
                get("/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val username = principal?.payload?.getClaim("username")?.asString()
                    val id = call.request.pathVariables["id"]
                    var file: File? = null
                    try {
                        username?.let {
                            id?.let {
                                val result = controller.retrieveImage(username, id.toInt())
                                result.onSuccess { image ->
                                    call.response.headers.append(
                                        HttpHeaders.ContentType,
                                        ContentType.Image.JPEG.toString()
                                    )

                                    call.respondFile(File(image.first.name)
                                        .apply { writeBytes(image.second) }
                                        .also { file = it })
                                }.onFailure {
                                    call.respond(HttpStatusCode.NotFound)
                                }
                            } ?: call.respond(HttpStatusCode.BadRequest, "invalid id: $it")
                        } ?: call.respond(HttpStatusCode.Unauthorized)
                    } catch (ex: NumberFormatException) {
                        call.respond(HttpStatusCode.BadRequest, ex.message ?: "Check the request")
                    } finally {
                        file?.delete()
                    }
                }
                post("/{id}/transform") {
                    val principal = call.principal<JWTPrincipal>()
                    val username = principal?.payload?.getClaim("username")?.asString()
                    try {
                        val id = call.pathParameters["id"]
                        val request = call.receive<ImageFormattingRequest>()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, "This id: $id is invalid")
                            return@post
                        }
                        if (username == null) {
                            call.respond(HttpStatusCode.Unauthorized, "User is unauthorised")
                            return@post
                        }
                        controller.retrieveImage(username, id.toInt()).onSuccess {
                            val outputImage = ImageFormatter().formatImage(it.second,request)
                            val file = File("filtered-${it.first.name}").also { it.writeBytes(outputImage) }
                            call.respondFile(file)
                            file.delete()
                        }.onFailure {
                            call.respond(HttpStatusCode.NotFound, "Image with id $id not found")
                        }
                    } catch (ex: NumberFormatException) {
                        call.respond(HttpStatusCode.BadRequest, "invalid id ${ex.message}.")
                    }
                }
            }
            route("/images") {
                get {
                    val principal = call.principal<JWTPrincipal>()
                    val username = principal?.payload?.getClaim("username")?.asString()

                    try {
                        //todo add pagination and limit logic
                        val page = call.request.queryParameters["page"]?.toInt() ?: 1
                        val limit = call.request.queryParameters["limit"]?.toInt() ?: 5

                        val result = controller.retrieveAll(username ?: "")
                        call.response.headers.append(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString()
                        )

                        call.respond(HttpStatusCode.OK, result.getOrThrow())
                    } catch (ex: NumberFormatException) {
                        call.respond(HttpStatusCode.BadRequest, ex.message ?: "Check the request")
                    }
                }
            }
        }
    }
}

    fun generateToken(username: String): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withExpiresAt(Instant.now().plusSeconds(90.minutes.inWholeSeconds))
        .withClaim("username", username)
        .sign(Algorithm.HMAC256(secret))
