package com.example

import com.example.authentication.AppController
import com.example.authentication.LoginRequest
import com.example.authentication.UNAUTHORISED
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(controller: AppController) {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = controller.login(request.username,request.password)
            if (response) {
                call.respondText("valid")
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
    }
}
