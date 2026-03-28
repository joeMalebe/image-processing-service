package com.example

import java.io.File

val secret by lazy { getSecret("secret") }
val dbPassword by lazy { getSecret("dbPassword") }
val dbUser by lazy { getSecret("dbUser") }
val dbName by lazy { getSecret("dbName") }

fun getSecret(name: String): String {
    val dockerSecret = File("run/secrets/$name.txt")
    if (dockerSecret.exists()) return dockerSecret.readText().trim()
    return System.getProperty("secret")
        ?: throw IllegalArgumentException("No system property found for $name")
}