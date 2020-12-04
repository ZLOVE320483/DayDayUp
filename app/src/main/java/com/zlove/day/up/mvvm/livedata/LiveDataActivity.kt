package com.zlove.day.up.mvvm.livedata

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.zlove.day.up.R
import kotlinx.android.synthetic.main.activity_livedata.*

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/3.
 * PS: Not easy to write code, please indicate.
 */
class LiveDataActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_livedata)

        val userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        userViewModel.mUserLiveData.observe(this, Observer<User> { user ->
            user_info.text = user?.toString()
        })

        post_value.setOnClickListener {
            userViewModel.mUserLiveData.postValue(User("zlove", 30))
        }

        NetworkLiveData.getInstance(this).observe(this, Observer {
            Log.d("NetworkLiveData", " NetworkInfo --- $it")
        })
    }
}