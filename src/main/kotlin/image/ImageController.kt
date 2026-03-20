package com.example.image

import com.example.database.ImageTable
import com.example.database.UserTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

var imageId = 0

interface IImageController {
    fun uploadImage(file: ByteArray,username: String, originalFileName: String = ""): Result<ImageMetaData>
    fun retrieveImage(username: String, id: Int): Result<Pair<ImageMetaData, ByteArray>>
    fun retrieveAll(username: String): Result<List<ImageMetaData>>
}

class ImageController(val database: ImageDataBase = ImageDatabaseImpl()) : IImageController {
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



class ImageDatabaseImpl : ImageDataBase {
    val database = mutableMapOf<String, MutableList<UserImage>>()
    override fun upload(
        file: ByteArray,
        username: String,
        originalFileName: String
    ): Result<ImageMetaData> {
        val metaData = ImageMetaData(imageId.toString(), username, originalFileName)

        val result = insertImage(UserImage(image = file, metaData))
        return result.mapCatching {
             ImageMetaData(it.toString(), username, originalFileName)
         }
    }

    override fun download(id: Int, username: String): Result<Pair<ImageMetaData, ByteArray>> {
        return getImage(id, username)
    }

    override fun retrieveAll(username: String): Result<List<ImageMetaData>> {
        return getAllImagesMetaData(username)
    }

    fun <T> loggedTransaction(block: JdbcTransaction.() -> T): T {
        return transaction {
            addLogger(StdOutSqlLogger)
            return@transaction block()
        }
    }

    private fun insertImage(userImage: UserImage): Result<Int> = loggedTransaction {
        val id = UserTable.select(UserTable.id).where { UserTable.name eq userImage.username }
            .singleOrNull()
        return@loggedTransaction id?.let {
            Result.success(ImageTable.insert {
                it[name] = userImage.imageMetaData.name
                it[url] = userImage.imageMetaData.url
                it[userId] = id[UserTable.id]
                it[imageBytes] = ExposedBlob(userImage.image)
            }[ImageTable.id])
        } ?: Result.failure(NoSuchElementException("User ${userImage.username} not found"))
    }

    private fun getAllImagesMetaData(username: String): Result<List<ImageMetaData>> = loggedTransaction {
        addLogger(StdOutSqlLogger)
        val id =
            UserTable.select(UserTable.id).where { UserTable.name eq username }.singleOrNull()
                ?.get(UserTable.id)
        return@loggedTransaction id?.let {
            Result.success( ImageTable.selectAll().where { ImageTable.userId eq id }.map {
                ImageMetaData(
                    it[ImageTable.id].toString(),
                    it[ImageTable.name],
                    it[ImageTable.url]
                )
            })
        } ?: Result.failure(NoSuchElementException("User $username not found "))
    }

    private fun getImage(imageId: Int, username: String): Result<Pair<ImageMetaData, ByteArray>> = loggedTransaction {
        addLogger(StdOutSqlLogger)
        val userId = UserTable.select(UserTable.id).where { UserTable.name eq username }.singleOrNull()
            ?.get(UserTable.id)
        return@loggedTransaction userId?.let {

            ImageTable.selectAll().where((ImageTable.userId eq userId) and (ImageTable.id eq imageId)).singleOrNull()?.let {

                Result.success( ImageMetaData(it[ImageTable.id].toString(), it[ImageTable.name], it[ImageTable.url]) to it[ImageTable.imageBytes].bytes)
            } ?: Result.failure(NoSuchElementException("No image found (id: $imageId) for user: $username"))
        } ?: Result.failure(NoSuchElementException("user: $username not found"))
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
        username: String,
        originalFileName: String
    ): Result<ImageMetaData>

    fun download(id: Int, username: String): Result<Pair<ImageMetaData, ByteArray>>

    fun retrieveAll(username: String): Result<List<ImageMetaData>>
}

@Serializable
data class ImageMetaData(val id: String, val name: String, val url: String)