package com.zlove.day.up.plugin

import android.content.Context

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2021/1/26.
 * PS: Not easy to write code, please indicate.
 */
class PluginManager private constructor() {

    private var mContext: Context? = null
    private var mPluginPath: String? = null

    companion object {

        private var instance: PluginManager? = null
            get() {
                if (field == null) {
                    field = PluginManager()
                }
                return field
            }

        @Synchronized
        fun get(): PluginManager {
            return instance!!
        }
    }

    fun init(context: Context, pluginPath: String) {
        this.mContext = context
        this.mPluginPath = pluginPath
    }

}