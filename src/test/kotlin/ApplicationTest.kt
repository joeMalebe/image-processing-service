package com.example

import com.example.authentication.AppController
import com.example.authentication.UNAUTHORISED
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
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

    @Test
    fun `when images with valid token then return 200`() = testApplication {
        application {
            module(AppController())
        }
        val response = client.post("/images") {
            val validToken =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwOi8vMC4wLjAuMDo4MDgwLyIsImF1ZCI6Imh0dHA6Ly8wLjAuMC4wOjgwODAvaGVsbG8iLCJ1c2VybmFtZSI6IkpvZSJ9.B10QPcDR2EYvl5seWuKe9hmvuu-a1A2cEUBZutae2zc"
            headers.append("Authorization", "Bearer $validToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `when images with invalid token then return 401`() = testApplication {
        application {
            module(AppController())
        }

        val response = client.post("/images") {
            headers.append("Authorisation", "Bearer invalid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

}
