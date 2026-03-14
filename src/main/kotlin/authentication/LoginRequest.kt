package com.example.authentication

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val userName: String)
