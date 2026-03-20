package com.example.image

interface ImageController {
    fun uploadImage(file: ByteArray,username: String, originalFileName: String = ""): Result<ImageMetaData>
    fun retrieveImage(username: String, id: Int): Result<Pair<ImageMetaData, ByteArray>>
    fun retrieveAll(username: String): Result<List<ImageMetaData>>
}

class ImageControllerImpl(val database: ImageDataBase = ImageDatabaseImpl()) : ImageController {
    override fun uploadImage(file: ByteArray, username: String, originalFileName: String): Result<ImageMetaData> {
        return database.upload(file, originalFileName = originalFileName, username = username)
    }

    override fun retrieveImage(username: String, id: Int): Result<Pair<ImageMetaData, ByteArray>> {
        return database.download(id = id, username = username)
    }

    override fun retrieveAll(username: String): Result<List<ImageMetaData>> {
        return database.retrieveAll(username)
    }
}