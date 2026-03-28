package com.example.image

import kotlinx.serialization.Serializable

@Serializable
data class ImageFormattingRequest(val resize: Resize? = null,
                           val crop: Crop? = null,
                           val rotate: Rotate? = null,
                           val flip: Flip? = null,
                           val format: Format? = null
)

@Serializable
data class Flip(val x: Boolean,val y: Boolean) {

}

@Serializable
data class Resize(val width: Int, val height: Int)

@Serializable
data class Crop(val width: Int, val height: Int, val x: Int, val y: Int)

@Serializable
data class Rotate(val angle: Int)

@Serializable
data class Format(val type: String)

@Serializable
data class Filters(val dihter: Boolean = false)

