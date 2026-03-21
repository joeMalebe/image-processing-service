package com.example.database

import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureMysqlDb() {
    val mysqldb = Database.connect(
        "jdbc:mysql://localhost:3306/imageDb",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = "@beast2468ds"
    )
}