package com.zlove.day.up.util

import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2021/1/27.
 * PS: Not easy to write code, please indicate.
 */
object FileUtils {
    fun copyAssetsData2File(filesDir: File, assets: AssetManager, fileName: String) {
        val file = File(filesDir, fileName)
        if (file.exists()) {
            return
        }
        var outputStream: OutputStream? = null
        var inputStream: InputStream? = null

        try {
            outputStream = FileOutputStream(file)
            inputStream = assets.open(fileName)
            val buffer = ByteArray(1024)
            var len = inputStream.read(buffer)
            while (len != -1) {
                outputStream.write(buffer, 0, len)
                len = inputStream.read(buffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                outputStream?.close()
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}