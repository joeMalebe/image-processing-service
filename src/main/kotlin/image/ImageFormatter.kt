package com.example.image

import com.sksamuel.scrimage.angles.Degrees
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.ImmutableImageLoader
import com.sksamuel.scrimage.nio.JpegWriter

class ImageFormatter {
    fun formatImage(
        inputFile: ByteArray,
        formatting: ImageFormattingRequest = ImageFormattingRequest()
    ): ByteArray {
        return flipTransformation.transform(
            rotateTransformation.transform(
                resizeTransformation.transform(
                    inputFile,
                    formatting
                ), formatting
            ), formatting
        )
    }
}

fun interface Transformation {
    fun transform(inputFile:ByteArray, formattingRequest: ImageFormattingRequest): ByteArray
}

val resizeTransformation = Transformation { inputFile, formattingRequest ->
    if (formattingRequest.resize == null) return@Transformation inputFile
    val result = ImmutableImageLoader().fromBytes(inputFile)
        .scaleTo(formattingRequest.resize.width, formattingRequest.resize.height)
    return@Transformation result.bytes(JpegWriter())
}

val rotateTransformation = Transformation { inputFile, formattingRequest ->
    if (formattingRequest.rotate == null) return@Transformation inputFile
    val result = ImmutableImageLoader().fromBytes(inputFile)
        .rotate(Degrees(formattingRequest.rotate.angle))
    return@Transformation result.bytes(JpegWriter())
}

val flipTransformation = Transformation { inputFile, formattingRequest ->
    if (formattingRequest.flip == null) return@Transformation inputFile
    var result = ImmutableImageLoader().fromBytes(inputFile)
    formattingRequest.flip.x.let {
        if (it) {
            result = result.flipX()
        }
    }
    formattingRequest.flip.y.let {
        if(it) {
            result = result.flipY()
        }
    }
    return@Transformation result.bytes(JpegWriter())
}




