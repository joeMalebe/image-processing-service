package com.example.image

import com.sksamuel.scrimage.metadata.ImageMetadata
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageFormatterTest {

    private val formatter = ImageFormatter()

    @Test
    fun `when image formatter then invoke function`() {
        formatter.formatImage(inputFile=  "".toByteArray())
    }

    @Test
    fun `when formatImage then return byteArray`() {
        val result = formatter.formatImage(inputFile = "".toByteArray())

        assertTrue( result is ByteArray)
    }

    @Test
    fun `when formatImage with no formatting then return copy of original`() {
        val input = File("src/test/resources/test.jpeg").readBytes()

        val outputFile = formatter.formatImage(input)

        assertEquals(input, outputFile)
    }

    @Test
    fun `when formatImage with resize formatter then return resized copy of original`() {
        val input = File("src/test/resources/test.jpeg").readBytes()
        val width = 500
        val height = 800

        val outputFile = formatter.formatImage(
            input,
            formatting = ImageFormattingRequest(resize = Resize(width, height))
        )
        val heightAndWidthTags =
            ImageMetadata.fromBytes(outputFile).tags().filter { it.name == "Image Width" || it.name == "Image Height" }

        assertEquals(width.toString(), heightAndWidthTags[1]?.rawValue)
        assertEquals(height.toString(), heightAndWidthTags[0]?.rawValue)
    }
}