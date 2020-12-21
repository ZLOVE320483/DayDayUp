package com.zlove.day.up

import android.app.Application
import android.content.Context
import android.util.Log
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.tencent.mmkv.MMKV

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/2.
 * PS: Not easy to write code, please indicate.
 */
class DayUpApplication : Application() {

    private var mRefWatcher: RefWatcher? = null

    companion object {
        fun getRefWatcher(context: Context): RefWatcher? {
            val application = context.applicationContext as DayUpApplication
            return application.mRefWatcher
        }
    }

    override fun onCreate() {
        super.onCreate()
        val dirPath = MMKV.initialize(this)
        Log.d("MMKV", "path --- $dirPath")
        if (!LeakCanary.isInAnalyzerProcess(this)) {
            mRefWatcher = LeakCanary.install(this)
        }
    }


}