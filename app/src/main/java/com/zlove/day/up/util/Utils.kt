package com.zlove.day.up.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/23.
 * PS: Not easy to write code, please indicate.
 */
inline fun <reified T : Activity> Activity.startActivity() {
    startActivity(Intent(this, T::class.java))
}

inline fun <reified F : Fragment> Context.newFragment(vararg args: Pair<String, String>): F {
    val bundle = Bundle()
    args.let {
        for (arg in args) {
            bundle.putString(arg.first, arg.second)
        }
    }
    return (FragmentFactory.loadFragmentClass(this.classLoader, F::class.java.name) as F).apply {
        arguments = bundle
    }
}