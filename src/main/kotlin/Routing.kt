package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.authentication.AppController
import com.example.authentication.LoginRequest
import com.example.authentication.UNAUTHORISED
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.http.headers
import io.ktor.http.headersOf
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Instant

const val secret = "secret"
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
                call.respondText(UNAUTHORISED)
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
            post("/images") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val param = call.receiveMultipart()

                param.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val result = controller.uploadImage(
                                part.provider().toByteArray(),
                                part.originalFileName ?: "$username-file"
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
            get("/images/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal?.payload?.getClaim("username")?.asString()
                val id = call.request.pathVariables["id"]

                try {
                    username?.let {
                        id?.let {
                            val result = controller.retrieveImage(username, id.toInt())
                            result.onSuccess { image ->
                                call.response.headers.append(
                                    HttpHeaders.ContentType,
                                    ContentType.Image.JPEG.toString()
                                )
                                call.respondFile(File(image.first.name).apply { writeBytes(image.second) })
                            }.onFailure {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } ?: call.respond(HttpStatusCode.BadRequest, "invalid id: $it")
                    } ?: call.respond(HttpStatusCode.Unauthorized)
                } catch (ex: NumberFormatException) {
                    call.respond(HttpStatusCode.BadRequest, ex.message ?: "Check the request")
                }
            }
        }
    }
}

fun generateToken(username: String): String = JWT.create()
    .withIssuer(issuer)
    .withAudience(audience).withExpiresAt(Instant.now().plusSeconds(600))
    .withClaim("username", username)
    .sign(Algorithm.HMAC256(secret))
