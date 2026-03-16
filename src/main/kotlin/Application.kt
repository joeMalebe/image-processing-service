package com.example

import com.example.authentication.AppController
import io.ktor.server.application.*
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

fun main(args: Array<String>) {
    embeddedServer(
        CIO,
        port = 8080,
        host = "0.0.0.0",

        module = {
            module(controller = AppController())
        }
    ).start(true)
}

fun Application.module(controller: AppController) {
    configureSerialization()
    configureHTTP()
    configureRouting(controller = controller)
}
