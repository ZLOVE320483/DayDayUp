package com.zlove.day.up.launchmode

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.zlove.day.up.FirstFragment
import com.zlove.day.up.R
import com.zlove.day.up.util.newFragment
import com.zlove.day.up.util.startActivity
import kotlinx.android.synthetic.main.activity_launch_mode.*

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/6.
 * PS: Not easy to write code, please indicate.
 */
class FirstSingleTopActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_mode)

        jump.setOnClickListener {
            startActivity<SecondSingleTopActivity>()
        }

        val fragment = newFragment<FirstFragment>(Pair("aaa", "bbb"))

        Log.d("LaunchMode", "FirstSingleTopActivity --- onCreate ---")
        Log.d("LaunchMode", "FirstSingleTopActivity --- hashcode --- ${hashCode()}")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("LaunchMode", "FirstSingleTopActivity --- onNewIntent ---")
    }

    override fun onStart() {
        super.onStart()
        Log.d("LaunchMode", "FirstSingleTopActivity --- onStart ---")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("LaunchMode", "FirstSingleTopActivity --- onRestart ---")
    }

    override fun onResume() {
        super.onResume()
        Log.d("LaunchMode", "FirstSingleTopActivity --- onResume ---")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d("LaunchMode", "FirstSingleTopActivity --- onWindowFocusChanged ---")
    }

    override fun onPause() {
        super.onPause()
        Log.d("LaunchMode", "FirstSingleTopActivity --- onPause ---")
    }

    override fun onStop() {
        super.onStop()
        Log.d("LaunchMode", "FirstSingleTopActivity --- onStop ---")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LaunchMode", "FirstSingleTopActivity --- onDestroy ---")
    }
}