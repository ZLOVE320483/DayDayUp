package com.zlove.day.up.plugin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zlove.day.up.R
import kotlinx.android.synthetic.main.activity_plugin_entry.*

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2021/1/28.
 * PS: Not easy to write code, please indicate.
 */
class PluginEntryActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plugin_entry)

        jump.setOnClickListener {
            val intent = Intent()
            intent.putExtra("user_name", "卡卡卡")
            intent.setClass(this@PluginEntryActivity, Class.forName("com.zlove.day.plugin.DayPluginActivity"))
            startActivity(intent)
        }
    }
}