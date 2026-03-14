package com.example

import com.example.authentication.AppController
import com.example.authentication.LoginRequest
import com.example.authentication.UNAUTHORISED
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module(AppController())
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `call sign up should return valid`() = testApplication {
        application {
            module(AppController())
        }
        val response = client.post("/sign-up") {
            contentType(ContentType.Application.Json)
            setBody("""{username:Joe, password:password}""")
        }

        assertEquals("valid", response.bodyAsText())
    }

    @Test
    fun `call login with http request`() = testApplication {
        application {
            module(AppController())
        }
        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{username:Admin, password:password}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(UNAUTHORISED, response.bodyAsText())
    }

    @Test
    fun `when signup has invalid request return bad request`() = testApplication {
        application {
            module(AppController())
        }

        val response = client.post ("/sign-up") {
            contentType(ContentType.Application.Json)
            setBody("""{sdf:sdf}""")
        }

        assertEquals(response.status, HttpStatusCode.BadRequest)
    }

    @Test
    fun `when signup has empty request values return bad request`() = testApplication {
        application {
            module(AppController())
        }

        val response = client.post ("/sign-up") {
            contentType(ContentType.Application.Json)
            setBody("""{username:"", password:""}""")
        }

        assertEquals(response.status, HttpStatusCode.BadRequest)
    }

    @Test
    fun `when login is successful return valid`() = testApplication {
        application {
            module(AppController())
        }
        client.post("/sign-up") {
            contentType(ContentType.Application.Json)
            setBody("""{username:Joe, password:password}""")
        }
        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{username:Joe, password:password}""")
        }

        assertEquals("valid", response.bodyAsText())
    }

}
