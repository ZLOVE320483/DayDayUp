package com.zlove.day.up

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.zlove.day.up.plugin.HookStartActivityUtils
import com.zlove.day.up.plugin.PluginManager
import com.zlove.day.up.util.FileUtils
import java.io.File

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/2.
 * PS: Not easy to write code, please indicate.
 */
class DayUpApplication : Application() {

    private var mPluginResource: Resources? = null

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        FileUtils.copyAssetsData2File(filesDir, assets, "day-plugin-debug.apk")
        val file = File(filesDir.absolutePath)
        var pluginPath = ""
        file.listFiles()?.forEach {
            if (it.name.endsWith(".apk")) {
                pluginPath = it.absolutePath
            }
        }
        println("pluginPath --- $pluginPath")
        PluginManager.getInstance().init(this, pluginPath)
        HookStartActivityUtils.hookStartActivity(this)
        PluginManager.getInstance().insertDex(this.classLoader)
        mPluginResource = PluginManager.getInstance().initPluginResource()
    }

    override fun getResources(): Resources {
        return if (mPluginResource == null) super.getResources() else mPluginResource!!
    }
}