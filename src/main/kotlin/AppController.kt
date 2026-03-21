package com.example

import com.example.authentication.Authentication
import com.example.authentication.AuthenticationController
import com.example.image.ImageController
import com.example.image.ImageControllerImpl

class AppController(
    authenticationController: AuthenticationController = AuthenticationController(),
    imageControllerImpl: ImageControllerImpl = ImageControllerImpl()
) : ImageController by imageControllerImpl,
    Authentication by authenticationController {
    private val privateKey = "sdfxcv3424ser33JJDF"


}