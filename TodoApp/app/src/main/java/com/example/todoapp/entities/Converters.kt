package com.example.todoapp.entities

import androidx.room.TypeConverter
import android.net.Uri
import android.util.Log

// Used for conversion from List<Uri> to String, and vice versa
class Converters {
    @TypeConverter
    fun stringToList(value: String): List<Uri> {
        val uriList = mutableListOf<Uri>()
        val parts = value.split(",").map { it.trim() }
        for (part in parts) {
            if (part.isNotBlank())
                uriList.add(Uri.parse(part))
        }
        Log.i("stringToList", "from $value we get $uriList")
        return uriList
    }

    @TypeConverter
    fun listToString(uriList: List<Uri>): String {
        val stringList = mutableListOf<String>()
        for (uri in uriList) {
            stringList.add(uri.toString())
        }
        Log.i("listToString",stringList.joinToString(separator = ","))
        return stringList.joinToString(separator = ",")
    }
}
