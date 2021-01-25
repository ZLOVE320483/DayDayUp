package com.zlove.day.up.map

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zlove.day.up.R
import org.greenrobot.eventbus.EventBus

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/12.
 * PS: Not easy to write code, please indicate.
 */
class MapTestActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}