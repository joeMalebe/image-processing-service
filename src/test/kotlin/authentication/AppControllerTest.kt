package com.example.authentication

import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AppControllerTest {

    //receive the http request from the user
    //if the user log in is success then create a jwt token
    //User the header and payload to calculate a signature.
    //encode the header.payload.signature as a base64 string
    //Jwt token is returned


    private val headers = """{"alg":"HS256","typ":"JWT","user-agent":"testing"}""".trim().trimIndent()
}