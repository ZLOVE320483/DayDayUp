package com.zlove.day.up.mvvm.livedata

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/3.
 * PS: Not easy to write code, please indicate.
 */
class UserViewModel : ViewModel() {

    val mUserLiveData = MutableLiveData<User>()

}