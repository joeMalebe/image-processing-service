package com.example.database

import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

object UserTable : Table("user") {
    val id = integer("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val name = varchar("name", 50)
    val password = varchar("password", 100)
}

object ImageTable : Table() {
    val id = integer("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val name = varchar("name", 50)
    val url = varchar("url", 100)
    val userId = reference("userId", UserTable.id)
    val imageBytes = registerColumn("imageBytes", CustomBlobColumnType("MEDIUMBLOB"))
}

class CustomBlobColumnType(private val typeName: String) : ColumnType<ExposedBlob>() {
    override fun sqlType(): String = typeName

    // This converts the DB value (usually a java.sql.Blob) back into ExposedBlob
    override fun valueFromDB(value: Any): ExposedBlob = when (value) {
        is ExposedBlob -> value
        is java.sql.Blob -> ExposedBlob(value.binaryStream)
        is ByteArray -> ExposedBlob(value)
        else -> error("Unexpected value of type Blob: $value")
    }

    // FIX: Extract the underlying bytes so the JDBC driver doesn't try to serialise the wrapper
    override fun notNullValueToDB(value: ExposedBlob): Any = value.bytes

    override fun nonNullValueToString(value: ExposedBlob): String = "?"
}