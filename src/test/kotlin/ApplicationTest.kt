package com.example

import com.example.authentication.AuthenticationController
import com.example.authentication.AuthenticationDatabase
import com.example.image.ImageControllerImpl
import com.example.image.ImageDataBase
import com.example.image.ImageMetaData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.Before
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ApplicationTest {

    private val mockImageDb = mock<ImageDataBase>()
    private val mockAuthDb = mock<AuthenticationDatabase>()
    private val controllerWithMockDb =
        AppController(imageControllerImpl = ImageControllerImpl(database = mockImageDb), authenticationController = AuthenticationController(mockAuthDb))

    @Before
    fun setUp() {
        System.setProperty("secret", "secret")
        System.setProperty("dbPassword", "database")
        System.setProperty("dbName", "testDb")
        System.setProperty("dbUser", "test")
    }

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
    fun `call sign up should return valid`() = testApplicationWithController {
        whenever { mockAuthDb.insertUser(any(),any()) }.thenReturn(1)
        val response = client.post("/sign-up") {
            contentType(ContentType.Application.Json)
            setBody("""{username:Joe, password:password}""")
        }

        assertEquals("valid", response.bodyAsText())
    }

    @Test
    fun `when signup has invalid request return bad request`() = testApplicationWithController {

        val response = client.post("/sign-up") {
            contentType(ContentType.Application.Json)
            setBody("""{sdf:sdf}""")
        }

        assertEquals( HttpStatusCode.BadRequest,response.status)
    }

    @Test
    fun `when signup has empty request values return bad request`() = testApplicationWithController {
        val response = client.post("/sign-up") {
            contentType(ContentType.Application.Json)
            setBody("""{username:"", password:""}""")
        }

        assertEquals( HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `when login is successful return valid`() = testApplicationWithController {
        whenever { mockAuthDb.isValidUser(any(), any()) }.thenReturn(true)
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

    fun testApplicationWithController(block:suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            application {
                module(controllerWithMockDb)
            }
            block()
        }
    }

    @Test
    fun `when images with valid token and no image then return 415`() = testApplicationWithController {
        val response = client.post("/image") {
            appendAuthorizationHeader()
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    private val testImage = File("src/test/resources/test.jpeg")

    @Test
    fun `when images has image in body then return ok`() = testApplicationWithController {
        whenever { mockImageDb.upload(any(), any(), any()) }.thenReturn(
            Result.success(
                ImageMetaData(
                    "1",
                    "test",
                    "test"
                )
            )
        )
        val response = client.post("/image") {
            appendAuthorizationHeader()
            val boundary = "WebAppBoundary"

            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("description", "image")
                        append(
                            "image",
                            testImage.readBytes(),
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
    fun `when images with invalid token then return 401`() = testApplicationWithController {

        val response = client.post("/image") {
            headers.append("Authorisation", "Bearer invalid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }


    @Test
    fun `when retrieve image is successful then return image and metadata`() = testApplicationWithController {
        val image = testImage.readBytes()
        val meta = ImageMetaData(id = "1", name = "test", url = "test")
        whenever { mockImageDb.download(2, "Joe") }.thenReturn(
            Result.success(
                Pair(
                    meta,
                    image
                )
            )
        )

        val response = client.get("/image/2") {
            appendAuthorizationHeader()

        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(image.decodeToString(), response.call.response.bodyAsBytes().decodeToString())
        assertEquals("image/jpeg", response.headers[HttpHeaders.ContentType])
    }

    @Test
    fun `when retrieve request has non number id then return bad request`() = testApplicationWithController {
        val image = testImage.readBytes()
        val meta = ImageMetaData(id = "1", name = "test", url = "test")
        whenever { mockImageDb.download(2, "test") }.thenReturn(
            Result.success(
                Pair(
                    meta,
                    image
                )
            )
        )

        val response = client.get("/image/fw") {
            appendAuthorizationHeader()

        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `when retrieve image with non existing id then return not found`() = testApplicationWithController {
        whenever { mockImageDb.download(2, "Joe") }.thenReturn(Result.failure(NoSuchElementException()))

        val response = client.get("/image/2") {
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
        testApplicationWithController {
            whenever { mockImageDb.retrieveAll("Joe") }.thenReturn(
                Result.success(
                    userImages
                )
            )
            val response = client.get("/images?page=1&limit=2") {
                appendAuthorizationHeader()
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("application/json", response.headers[HttpHeaders.ContentType])
            assertEquals(prettyJson.encodeToString(userImages), response.body())
        }
    }

    @Test
    fun `when retrieve image list with invalid page then return bad request`() = testApplicationWithController {

        val response = client.get("/images?page=a&limit=2") {
            appendAuthorizationHeader()
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

    }

    @Test
    fun `when retrieve image list with invalid limit then return bad request`() = testApplicationWithController {

        val response = client.get("/images?page=3&limit=b") {
            appendAuthorizationHeader()
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

    }

    @Test
    fun `when transform image is null then return 404`() = testApplicationWithController {
        whenever { mockImageDb.download(any(), any()) }.thenReturn(
            Result.failure(
                RuntimeException("problem")
            )
        )
        val response = client.post("/image/1/transform") {
            appendAuthorizationHeader()
            contentType(ContentType.Application.Json)
            val request = """
                {resize: { width: 100, height: 400}}
                    
            """
            setBody(request.trimIndent())
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `when transform with non number id then return bad request`() = testApplicationWithController {
        whenever { mockImageDb.download(any(), any()) }.thenReturn(
            Result.failure(
                RuntimeException("problem")
            )
        )
        val response = client.post("/image/s/transform") {
            appendAuthorizationHeader()
            contentType(ContentType.Application.Json)
            val request = """
                {resize: { width: 100, height: 400}}
                    
            """
            setBody(request.trimIndent())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `when transform image with resize formatting then return transformed image`() = testApplicationWithController {
        val meta = ImageMetaData(id = "1", name = "test", url = "test")
        val original = testImage.readBytes()
        whenever { mockImageDb.download(any(), any()) }.thenReturn(
            Result.success(
                Pair(
                    meta,
                    original
                )
            )
        )
        val response = client.authorizedPost("/image/3/transform") {
            contentType(ContentType.Application.Json)
            val request = """
                {resize: { width: 100, height: 400}}
                    
            """
            setBody(request.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertNotEquals(original.decodeToString(), response.bodyAsBytes().decodeToString())
    }

    @Test
    fun `when transform with empty formatting request then return copy of image`() = testApplicationWithController {
        val meta = ImageMetaData(id = "1", name = "test", url = "test")
        val original = testImage.readBytes()
        whenever { mockImageDb.download(any(), any()) }.thenReturn(
            Result.success(
                Pair(
                    meta,
                    original
                )
            )
        )
        val response = client.authorizedPost("/image/1/transform") {
            contentType(ContentType.Application.Json)
            val request = """
                {}
            """
            setBody(request.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(original.decodeToString(), response.bodyAsBytes().decodeToString())
    }

    private suspend fun HttpClient.authorizedPost(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        return this.post(urlString) {
            appendAuthorizationHeader()
            block()
        }
    }

    private fun HttpRequestBuilder.appendAuthorizationHeader() {
        val validToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwOi8vMC4wLjAuMDo4MDgwLyIsImF1ZCI6Imh0dHA6Ly8wLjAuMC4wOjgwODAvaGVsbG8iLCJ1c2VybmFtZSI6IkpvZSJ9.B10QPcDR2EYvl5seWuKe9hmvuu-a1A2cEUBZutae2zc"
        headers.append("Authorization", "Bearer $validToken")
    }
}
