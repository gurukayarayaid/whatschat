package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType

class Converters {
    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = runCatching {
        MessageType.valueOf(value)
    }.getOrDefault(MessageType.TEXT)

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus): String = value.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = runCatching {
        MessageStatus.valueOf(value)
    }.getOrDefault(MessageStatus.READ)
}
