package com.example.authentication

import io.ktor.http.Headers
import io.ktor.util.Platform
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

class AppController(authenticationController: AuthenticationController = AuthenticationController()): Authentication by authenticationController{
    private val privateKey = "sdfxcv3424ser33JJDF"


}