package com.zlove.day.up.launchmode

import android.annotation.SuppressLint
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
class SecondSingleTopActivity : AppCompatActivity() {

    @SuppressLint("CI_ByteDanceKotlinRules_Start_Activity_With_Companion")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_mode)

        jump.setOnClickListener {
            startActivity(Intent(this@SecondSingleTopActivity, SecondSingleTopActivity::class.java))
        }

        Log.d("LaunchMode", "SecondSingleTopActivity --- onCreate ---")
        Log.d("LaunchMode", "SecondSingleTopActivity --- hashcode --- ${hashCode()}")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("LaunchMode", "SecondSingleTopActivity --- onNewIntent ---")
    }

    override fun onStart() {
        super.onStart()
        Log.d("LaunchMode", "SecondSingleTopActivity --- onStart ---")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("LaunchMode", "SecondSingleTopActivity --- onRestart ---")
    }

    override fun onResume() {
        super.onResume()
        Log.d("LaunchMode", "SecondSingleTopActivity --- onResume ---")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d("LaunchMode", "SecondSingleTopActivity --- onWindowFocusChanged ---")
    }

    override fun onPause() {
        super.onPause()
        Log.d("LaunchMode", "SecondSingleTopActivity --- onPause ---")
    }

    override fun onStop() {
        super.onStop()
        Log.d("LaunchMode", "SecondSingleTopActivity --- onStop ---")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LaunchMode", "SecondSingleTopActivity --- onDestroy ---")
    }
}