package com.zlove.day.up.mvvm.livedata

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/3.
 * PS: Not easy to write code, please indicate.
 */
data class User(val userName: String, val userAge: Int) {

    override fun toString(): String {
        return "userName: $userName, userAge: $userAge"
    }
}