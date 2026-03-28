package com.example.database

import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database

val dbPassword = System.getProperty("dbPassword")!!
val dbUser = System.getProperty("dbUser")!!
val dbName = System.getProperty("dbName")!!

fun Application.configureMysqlDb() {
    Database.connect(
        "jdbc:mysql://db:3306/$dbName",
        driver = "com.mysql.cj.jdbc.Driver",
        user = dbUser,
        password = dbPassword
    )
}