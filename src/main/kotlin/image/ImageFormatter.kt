package com.example.image

import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.ImmutableImageLoader
import com.sksamuel.scrimage.nio.JpegWriter

class ImageFormatter {
    fun formatImage(
        inputFile: ByteArray,
        formatting: ImageFormattingRequest = ImageFormattingRequest()
    ): ByteArray {
        if (formatting.resize == null) return inputFile
        val result = ImmutableImageLoader().fromBytes(inputFile)
            .resizeTo(formatting.resize.width, formatting.resize.height)
        ImageSource.of(inputFile.inputStream())
        return result.bytes(JpegWriter())
    }
}