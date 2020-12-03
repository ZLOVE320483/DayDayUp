package com.zlove.day.up.mvvm.lifecycle

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/3.
 * PS: Not easy to write code, please indicate.
 */
class FeedsComponent constructor(lifecycleOwner: LifecycleOwner) {

    init {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {

            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when(event) {
                    Lifecycle.Event.ON_CREATE -> onCreate()
                    Lifecycle.Event.ON_START -> onStart()
                    Lifecycle.Event.ON_RESUME -> onResume()
                    Lifecycle.Event.ON_PAUSE -> onPause()
                    Lifecycle.Event.ON_STOP -> onStop()
                    Lifecycle.Event.ON_DESTROY -> onDestroy()
                    else -> {}
                }
            }

        })
    }


    private fun onCreate() {
        Log.d("FeedsComponent", "FeedsComponent --- onCreate ---")
    }

    private fun onStart() {
        Log.d("FeedsComponent", "FeedsComponent --- onStart ---")
    }

    private fun onResume() {
        Log.d("FeedsComponent", "FeedsComponent --- onResume ---")
    }

    private fun onPause() {
        Log.d("FeedsComponent", "FeedsComponent --- onPause ---")
    }

    private fun onStop() {
        Log.d("FeedsComponent", "FeedsComponent --- onStop ---")
    }

    private fun onDestroy() {
        Log.d("FeedsComponent", "FeedsComponent --- onDestroy ---")
    }

    fun say() {
        Log.d("FeedsComponent", "Hello World!")
    }
}