package com.example.image

import kotlinx.serialization.Serializable

var imageId = 0

interface IImageController {
    fun uploadImage(file: ByteArray, originalFileName: String = ""): Result<ImageMetaData>
    fun retrieveImage(username: String, id: Int): Result<Pair<ImageMetaData, ByteArray>>
    fun retrieveAll(username: String): Result<List<ImageMetaData>>
}

class ImageController(val database: ImageDataBase = ImageDatabaseImpl()) : IImageController {
    override fun uploadImage(file: ByteArray, originalFileName: String): Result<ImageMetaData> {
        return database.upload(file, originalFileName = originalFileName)
    }

    override fun retrieveImage(username: String, id: Int): Result<Pair<ImageMetaData, ByteArray>> {
        return database.download(id = id, username = username)
    }

    override fun retrieveAll(username: String): Result<List<ImageMetaData>> {
        return database.retrieveAll(username)
    }
}

class ImageDatabaseImpl : ImageDataBase {
    val database = mutableMapOf<String, MutableList<UserImage>>()
    override fun upload(
        file: ByteArray,
        username: String,
        originalFileName: String
    ): Result<ImageMetaData> {
        val metaData = ImageMetaData(imageId.toString(), username, originalFileName)
        if (database.containsKey(username)) {
            database[username]?.add(UserImage(image = file, metaData))
        } else {
            database[username] =
                mutableListOf(UserImage(image = file, metaData))

        }
        return Result.success(metaData).also {
            imageId++
        }
    }

    override fun download(id: Int, username: String): Result<Pair<ImageMetaData, ByteArray>> {
        return database[username]?.firstOrNull { it.id == id }?.let {
            Result.success(
                it.imageMetaData to it.image
            )
        } ?: Result.failure(NoSuchElementException())
    }

    override fun retrieveAll(username: String): Result<List<ImageMetaData>> {
        return Result.success(database[username]?.map {
            it.imageMetaData
        } ?: listOf())
    }
}

data class UserImage(val image: ByteArray, val username: String, val id: Int, val imageMetaData: ImageMetaData) {
    constructor(image: ByteArray, imageMetaData: ImageMetaData) : this(
        image,
        imageMetaData.name,
        imageMetaData.id.toInt(),
        imageMetaData
    )
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
    fun upload(
        file: ByteArray,
        username: String = "Joe",
        originalFileName: String
    ): Result<ImageMetaData>

    fun download(id: Int, username: String): Result<Pair<ImageMetaData, ByteArray>>

    fun retrieveAll(username: String): Result<List<ImageMetaData>>
}

@Serializable
data class ImageMetaData(val id: String, val name: String, val url: String)