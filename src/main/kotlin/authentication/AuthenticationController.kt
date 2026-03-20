package com.example.authentication

import com.example.database.UserTable
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.math.BigInteger
import java.security.MessageDigest

const val UNAUTHORISED = "unauthorised"
interface Authentication {
    fun signUp(username: String, password: String): Boolean
    fun login(username: String, password: String): Boolean
}

interface AuthenticationDatabase {
    fun insertUser(username: String, userPassword: String): Int
    fun isValidUser(username: String, hashedPassword: String): Boolean
}

class AuthenticationDatabaseImpl() : AuthenticationDatabase {
    override fun insertUser(username: String, userPassword: String): Int {
        return transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(UserTable)

            return@transaction UserTable.insert {
                it[name] = username
                it[password] = userPassword
            } get UserTable.id
        }
    }

    override fun isValidUser(username: String, hashedPassword: String): Boolean {
        return transaction {
            addLogger(StdOutSqlLogger)

            return@transaction UserTable.select(UserTable.password)
                .where { (UserTable.name eq username) and (UserTable.password eq hashedPassword) }
                .any()

        }
    }
}

class AuthenticationController(val database: AuthenticationDatabase = AuthenticationDatabaseImpl()) :
    Authentication {
    override fun signUp(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) {
            return false
        } else {
            database.insertUser(username, sha256Hash(password))
            return true
        }
    }

    override fun login(username: String, password: String) =
        database.isValidUser(username, sha256Hash(password))
}

fun sha256Hash(password: String) =
    String.format("%064x", BigInteger(1, MessageDigest.getInstance("SHA-256").apply {
        update(password.toByteArray())
    }.digest()))
