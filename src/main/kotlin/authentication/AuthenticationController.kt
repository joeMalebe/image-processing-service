package com.example.authentication

import java.math.BigInteger
import java.security.MessageDigest
const val UNAUTHORISED = "unauthorised"
interface Authentication {
    fun signUp(username: String, password: String): Boolean
    fun login(username: String, password: String): Boolean
}

class AuthenticationController(val database: MutableMap<String, String> = mutableMapOf()) :
    Authentication {
    override fun signUp(username: String, password: String) =
        if (username.isBlank() || password.isBlank()) {
            false
        } else {
            database[username] = sha256Hash(password)
            true
        }

    override fun login(username: String, password: String) =
        database.containsKey(username) && database[username] == sha256Hash(password)
}

fun sha256Hash(password: String) =
    String.format("%064x", BigInteger(1, MessageDigest.getInstance("SHA-256").apply {
        update(password.toByteArray())
    }.digest()))
