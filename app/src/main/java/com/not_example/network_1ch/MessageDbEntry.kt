package com.not_example.network_1ch

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "messages")
@TypeConverters(Converters::class)
@JsonClass(generateAdapter = true)
data class MessageDbEntry(
    @PrimaryKey val id: Long,
    val time: Long,
    val from: String,
    val data: Data,
    var prevMki: Long?, // previous min known id
    var nextMki: Long? // next max known id
) {
    init {
        if (data.image != null) {
            data.image.filename = "img$id.png"
        }
    }
}

@JsonClass(generateAdapter = true)
data class Image(val link: String, var filename: String? = null)

@JsonClass(generateAdapter = true)
data class Text(val text: String)

@JsonClass(generateAdapter = true)
data class Data(@Json(name = "Image") val image: Image?, @Json(name = "Text") val text: Text?)

class Converters {
    @TypeConverter
    fun stringToData(value: String?): Data? {
        return if (value?.first() == 'i')
            Data(Image(value.drop(2)), null)
        else if (value?.first() == 't')
            Data(null, Text(value.drop(2)))
        else
            null
    }

    @TypeConverter
    fun dataToString(data: Data?): String? {
        return if (data?.image != null)
            "i+${data.image.link}"
        else if (data?.text != null)
            "t+${data.text.text}"
        else
            null
    }
}

