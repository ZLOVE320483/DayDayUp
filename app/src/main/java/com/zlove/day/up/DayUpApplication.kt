package com.zlove.day.up

import android.app.Application
import android.util.Log
import com.tencent.mmkv.MMKV

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/2.
 * PS: Not easy to write code, please indicate.
 */
class DayUpApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val dirPath = MMKV.initialize(this)
        Log.d("MMKV", "path --- $dirPath")
    }
}