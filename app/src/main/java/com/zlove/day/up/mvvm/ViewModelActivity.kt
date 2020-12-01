package com.zlove.day.up.mvvm

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.zlove.day.up.R

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/1.
 * PS: Not easy to write code, please indicate.
 */
class ViewModelActivity: FragmentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewmodel)

        Log.d("ViewModelActivity", "onCreate --- hashcode --- ${hashCode()}")

        val mainViewModel= ViewModelProvider(this).get(MainViewModel::class.java)

        Log.d("ViewModelActivity", "onCreate --- $mainViewModel")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d("ViewModelActivity", "onDestroy --- hashcode --- ${hashCode()}")
    }
}