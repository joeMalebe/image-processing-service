package com.example.image

import kotlinx.serialization.Serializable

var imageId = 0

interface IImageController {
    fun uploadImage(file: ByteArray, originalFileName: String = ""): Result<ImageMetaData>
}

class ImageController(val database: ImageDataBase = ImageDatabaseImpl()) : IImageController {
    override fun uploadImage(file: ByteArray, originalFileName: String): Result<ImageMetaData> {
      return database.upload(file, originalFileName = originalFileName)
    }
}

class ImageDatabaseImpl : ImageDataBase {
    val database = mutableMapOf<String,  MutableList<UserImage>>()
    override fun upload(file: ByteArray, username: String, originalFileName: String): Result<ImageMetaData> {
        if(database.containsKey(username)) {
            database[username]?.add(UserImage(image = file, username = username, id = imageId))
        } else {
            database[username] = mutableListOf(UserImage(image = file, username = username, id = imageId))

        }
        return Result.success(ImageMetaData(imageId.toString(),username,originalFileName)).also {
            imageId++
        }
    }

}

data class UserImage(val image: ByteArray, val username: String, val id: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserImage

        if (!image.contentEquals(other.image)) return false
        if (username != other.username) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = image.contentHashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }
}


interface ImageDataBase {
    fun upload(file: ByteArray, username: String = "Joe", originalFileName: String):Result<ImageMetaData>
}

@Serializable
data class ImageMetaData(val id: String, val name: String, val url: String)