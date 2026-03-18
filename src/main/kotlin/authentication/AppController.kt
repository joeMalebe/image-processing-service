package com.example.authentication

import com.example.image.IImageController
import com.example.image.ImageController
import io.ktor.http.Headers
import io.ktor.util.Platform
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

class AppController(
    authenticationController: AuthenticationController = AuthenticationController(),
    imageController: ImageController = ImageController()
) : IImageController by imageController,
    Authentication by authenticationController {
    private val privateKey = "sdfxcv3424ser33JJDF"


}