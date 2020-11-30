package com.zlove.day.up.retrofit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/11/30.
 * PS: Not easy to write code, please indicate.
 */
interface GitHubApiService {

    @GET("user/{user}/repos")
    fun listRepos(@Path("user") user: String?): Call<List<Any>>
}