package com.example.database

import com.example.dbName
import com.example.dbPassword
import com.example.dbUser
import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database.Companion.connect


fun Application.configureMysqlDb() {
    connect(
        "jdbc:mysql://db:3306/$dbName",
        driver = "com.mysql.cj.jdbc.Driver",
        user = dbUser,
        password = dbPassword
    )
}