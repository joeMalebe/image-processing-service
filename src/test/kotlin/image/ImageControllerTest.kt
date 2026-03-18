package com.example.image

import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals


class ImageControllerTest {
    val imageDB:ImageDataBase = mock()

    private val controller = ImageController(database = imageDB)

    @Test
    fun `upload image invoke`() {
        controller.uploadImage(file = byteArrayOf())
    }

    @Test
    fun `when uploadImage successful then invoke database upload`() {
        whenever(imageDB.upload(file = any(), username = any(), originalFileName = any())).thenReturn(Result.success(ImageMetaData(id = "1", name = "test", url = "test")))

         controller.uploadImage(file = byteArrayOf())

        verify(imageDB,times(1)).upload(file = any(), username = any(), originalFileName = any())
    }

    @Test
    fun `when uploadImage successful then return ImageMetaData`() {
        whenever(imageDB.upload(file = any(), username = any(), originalFileName = any())).thenReturn(Result.success(ImageMetaData(id = "1", name = "test", url = "test")))

        val result = controller.uploadImage(file = byteArrayOf())

        assertEquals(ImageMetaData(id = "1", name = "test", url = "test"),result.getOrThrow())
    }

    @Test
    fun `when uploadImage is unsuccessful then return error`() {
        whenever(imageDB.upload(file = any(), username = any(), originalFileName = any())).thenReturn(Result.failure(RuntimeException("Hello there")))

        val result = controller.uploadImage(file = byteArrayOf())

        result.onFailure {
            assertEquals("Hello there",it.message)
        }
    }

    @Test
    fun `when retrieve image is successful then return image`() {
        whenever { imageDB.download(any(), any()) }.thenReturn(Result.success(ImageMetaData(id = "1", name = "test", url = "test") to "sdfs".toByteArray()))

        val result = controller.retrieveImage(username = "Joe", id = 1)

        assertEquals("sdfs",result.getOrThrow().second.decodeToString())
    }

    @Test
    fun `when retrieve image is unsuccessful then return failed result`() {
        whenever { imageDB.download(any(), any()) }.thenReturn(Result.failure(RuntimeException("Hello there")))

        val result = controller.retrieveImage(username = "Joe", id = 1)

        result.onFailure {
            assertEquals("Hello there",it.message)
        }
    }
}