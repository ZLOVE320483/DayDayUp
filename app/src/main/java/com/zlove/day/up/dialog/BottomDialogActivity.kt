package com.zlove.day.up.dialog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zlove.day.up.R
import kotlinx.android.synthetic.main.activity_bottom_dialog.*

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/17.
 * PS: Not easy to write code, please indicate.
 */
class BottomDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_dialog)

        show.setOnClickListener {
            val fragment = BottomDialogFragment.newInstance()
            supportFragmentManager.beginTransaction().add(fragment, "").commitAllowingStateLoss()
            show.post {
                fragment.setText("hello word")
            }
        }
    }
}