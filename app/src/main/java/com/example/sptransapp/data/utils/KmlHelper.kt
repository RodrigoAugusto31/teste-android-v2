package com.example.sptransapp.data.utils

import okhttp3.ResponseBody
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

object KmlHelper {
    fun extractKmlFromKmz(responseBody: ResponseBody): ByteArrayInputStream? {
        try {
            val zipInputStream = ZipInputStream(responseBody.byteStream())
            var entry = zipInputStream.nextEntry

            while (entry != null) {
                if (entry.name.lowercase().endsWith(".kml")) {
                    val buffer = ByteArray(1024)
                    val outputStream = ByteArrayOutputStream()
                    var len: Int
                    while (zipInputStream.read(buffer).also { len = it } > 0) {
                        outputStream.write(buffer, 0, len)
                    }

                    return ByteArrayInputStream(outputStream.toByteArray())
                }
                entry = zipInputStream.nextEntry
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
