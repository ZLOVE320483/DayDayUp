package com.zlove.day.up.mmkv

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.tencent.mmkv.MMKV
import com.zlove.day.up.R
import kotlinx.android.synthetic.main.activity_mmkv.*
import java.util.*

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/5.
 * PS: Not easy to write code, please indicate.
 */
class MMKVActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mmkv)

        val kv = MMKV.defaultMMKV();

        add.setOnClickListener {
            // 增
            kv.encode("bool", true)
            Log.d("MMKV", "bool --- ${kv.decodeBool("bool")}")

            kv.encode("int", Int.MIN_VALUE)
            Log.d("MMKV", "int --- ${kv.decodeInt("int")}")

            kv.encode("long", Long.MAX_VALUE)
            Log.d("MMKV", "long --- ${kv.decodeLong("long")}")

            kv.encode("float", 3.14f)
            Log.d("MMKV", "float --- ${kv.decodeFloat("float")}")

            kv.encode("double", Double.MAX_VALUE)
            Log.d("MMKV", "double --- ${kv.decodeDouble("double")}")

            kv.encode("string", "Hello MMVK")
            Log.d("MMKV", "string --- ${kv.decodeString("string")}")
        }

        delete.setOnClickListener {
            // 删
            kv.removeValueForKey("bool")
            Log.d("MMKV", "bool --- ${kv.decodeBool("bool")}")
            kv.removeValuesForKeys(arrayOf("int", "long"))
            Log.d("MMKV", "allKeys --- ${Arrays.toString(kv.allKeys())}")
        }

    }
}