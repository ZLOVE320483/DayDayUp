package com.zlove.day.up.mvvm.livedata

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import androidx.lifecycle.LiveData

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/4.
 * PS: Not easy to write code, please indicate.
 */
class NetworkLiveData private constructor(context: Context): LiveData<NetworkInfo>() {

    private val mContext: Context = context.applicationContext
    private val mNetworkReceiver = NetworkReceiver()
    private val mIntentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

    companion object {
        var mNetworkInfo: NetworkInfo? = null

        @Volatile
        private var instance: NetworkLiveData? = null

        fun getInstance(context: Context): NetworkLiveData {
            return instance?: synchronized(this) {
                instance?: NetworkLiveData(context).also { instance = it }
            }
        }
    }

    override fun onActive() {
        super.onActive()
        Log.d("NetworkLiveData", "--- onActive ---")
        mContext.registerReceiver(mNetworkReceiver, mIntentFilter)
    }

    override fun onInactive() {
        super.onInactive()
        Log.d("NetworkLiveData", "--- onInactive ---")
        mContext.unregisterReceiver(mNetworkReceiver)
    }



    class NetworkReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context?, p1: Intent?) {
            context?.run {
                val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = manager.activeNetworkInfo
                getInstance(this).setValue(activeNetwork)
            }
        }
    }
}