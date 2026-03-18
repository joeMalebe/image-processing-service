package com.example

import com.example.authentication.AppController
import com.example.authentication.UNAUTHORISED
import com.example.image.ImageController
import com.example.image.ImageDataBase
import com.example.image.ImageMetaData
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    private val mockDb = mock<ImageDataBase>()
    private val controllerWithMockDb =
        AppController(imageController = ImageController(database = mockDb))

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

        val response = client.post("/sign-up") {
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

        val response = client.post("/sign-up") {
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
    fun `when images with valid token and no image then return 415`() = testApplication {
        application {
            module(AppController())
        }
        val response = client.post("/images") {
            appendAuthorizationHeader()
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    @Test
    fun `when images has image in body then return ok`() = testApplication {
        application {
            module(AppController())
        }
        val response = client.post("/images") {
            appendAuthorizationHeader()
            val boundary = "WebAppBoundary"

            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("description", "image")
                        append(
                            "image",
                            File("src/test/resources/test.jpeg").readBytes(),
                            Headers.build {
                                append(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
                                append(HttpHeaders.ContentDisposition, "filename=\"test.jpg\"")
                            })
                    },
                    boundary = boundary,
                    contentType = ContentType.MultiPart.FormData.withParameter("boundary", boundary)
                )
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("application/json", response.headers[HttpHeaders.ContentType])
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


    @Test
    fun `when retrieve image is successful then return image and metadata`() = testApplication {
        val image = File("src/test/resources/test.jpeg").readBytes()
        val meta = ImageMetaData(id = "1", name = "test", url = "test")
        whenever { mockDb.download(2, "Joe") }.thenReturn(
            Result.success(
                Pair(
                    meta,
                    image
                )
            )
        )
        application {
            module(controllerWithMockDb)
        }

        val response = client.get("/images/2") {
            appendAuthorizationHeader()

        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(image.decodeToString(), response.call.response.bodyAsBytes().decodeToString())
        assertEquals("image/jpeg", response.headers[HttpHeaders.ContentType])
    }

    @Test
    fun `when retrieve request has non number id then return bad request`() = testApplication {
        val image = File("src/test/resources/test.jpeg").readBytes()
        val meta = ImageMetaData(id = "1", name = "test", url = "test")
        whenever { mockDb.download(2, "test") }.thenReturn(
            Result.success(
                Pair(
                    meta,
                    image
                )
            )
        )
        application {
            module(controllerWithMockDb)
        }

        val response = client.get("/images/fw") {
            appendAuthorizationHeader()

        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `when retrieve image with non existing id then return not found`() = testApplication {
        whenever { mockDb.download(2, "Joe") }.thenReturn(Result.failure(NoSuchElementException()))
        application {
            module(controllerWithMockDb)
        }

        val response = client.get("/images/2") {
            appendAuthorizationHeader()

        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `when retrieve image list is successful then return list`() {
        val prettyJson = Json {
            prettyPrint = true
        }
        val userImages = listOf(
            ImageMetaData(id = "1", name = "test", url = "test"),
            ImageMetaData(id = "5", name = "test2", url = "test2")
        )
        testApplication {
            whenever { mockDb.retrieveAll("Joe") }.thenReturn(
                Result.success(
                    userImages
                )
            )
            application {
                module(controllerWithMockDb)
            }
            val response = client.get("/images?page=1&limit=2") {
                appendAuthorizationHeader()
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("application/json", response.headers[HttpHeaders.ContentType])
            assertEquals(prettyJson.encodeToString(userImages), response.body())
        }
    }

    @Test
    fun `when retrieve image list with invalid page then return bad request`() = testApplication {
        application {
            module(controllerWithMockDb)
        }

        val response = client.get("/images?page=a&limit=2") {
            appendAuthorizationHeader()
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

    }

    @Test
    fun `when retrieve image list with invalid limit then return bad request`() = testApplication {
        application {
            module(controllerWithMockDb)
        }

        val response = client.get("/images?page=3&limit=b") {
            appendAuthorizationHeader()
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

    }

    private fun HttpRequestBuilder.appendAuthorizationHeader() {
        val validToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwOi8vMC4wLjAuMDo4MDgwLyIsImF1ZCI6Imh0dHA6Ly8wLjAuMC4wOjgwODAvaGVsbG8iLCJ1c2VybmFtZSI6IkpvZSJ9.B10QPcDR2EYvl5seWuKe9hmvuu-a1A2cEUBZutae2zc"
        headers.append("Authorization", "Bearer $validToken")
    }


}
