package com.example.authentication

import java.math.BigInteger
import java.security.MessageDigest

class AuthenticationController(val database: MutableMap<String, String> = mutableMapOf()) {
    fun signUp(username: String, password: String) {
        database[username] = hashedPassword(password)
    }

    private fun hashedPassword(password: String) =
        String.format("%064x", BigInteger(1, MessageDigest.getInstance("SHA-256").apply {
            update(password.toByteArray())
        }.digest()))


    fun login(username: String, password: String) =
        database.containsKey(username) && database[username] == hashedPassword(password)

}