package com.zlove.day.up.mvvm.lifecycle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zlove.day.up.R

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/3.
 * PS: Not easy to write code, please indicate.
 */
class LifecycleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lifecycle)

        val feedsComponent = FeedsComponent()
        val musicComponent = MusicComponent()
        lifecycle.addObserver(feedsComponent)
        lifecycle.addObserver(musicComponent)

        feedsComponent.say()
        musicComponent.say()
    }
}