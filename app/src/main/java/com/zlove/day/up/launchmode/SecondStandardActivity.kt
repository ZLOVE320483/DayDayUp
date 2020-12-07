package com.zlove.day.up.launchmode

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.zlove.day.up.R
import kotlinx.android.synthetic.main.activity_launch_mode.*

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/6.
 * PS: Not easy to write code, please indicate.
 */
class SecondStandardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_mode)

        jump.setOnClickListener {
            startActivity(Intent(this@SecondStandardActivity, FirstStandardActivity::class.java))
        }

        Log.d("LaunchMode", "SecondStandardActivity --- onCreate ---")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("LaunchMode", "SecondStandardActivity --- onNewIntent ---")
    }

    override fun onStart() {
        super.onStart()
        Log.d("LaunchMode", "SecondStandardActivity --- onStart ---")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("LaunchMode", "SecondStandardActivity --- onRestart ---")
    }

    override fun onResume() {
        super.onResume()
        Log.d("LaunchMode", "SecondStandardActivity --- onResume ---")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d("LaunchMode", "SecondStandardActivity --- onWindowFocusChanged ---")
    }

    override fun onPause() {
        super.onPause()
        Log.d("LaunchMode", "SecondStandardActivity --- onPause ---")
    }

    override fun onStop() {
        super.onStop()
        Log.d("LaunchMode", "SecondStandardActivity --- onStop ---")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LaunchMode", "SecondStandardActivity --- onDestroy ---")
    }
}