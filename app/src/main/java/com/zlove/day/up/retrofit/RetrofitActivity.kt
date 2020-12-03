package com.zlove.day.up.retrofit

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.zlove.day.up.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/11/30.
 * PS: Not easy to write code, please indicate.
 */
class RetrofitActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_retrofit)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GitHubApiService::class.java)
        val repos = service.listRepos("ZLOVE32048")

        repos.enqueue(object : Callback<List<Any>> {

            override fun onFailure(call: Call<List<Any>>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<List<Any>>, response: Response<List<Any>>) {
                Log.d("RetrofitActivity", "response.code() = ${response.code()}")
            }

        })
    }
}