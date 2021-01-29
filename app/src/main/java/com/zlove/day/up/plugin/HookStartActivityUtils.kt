package com.zlove.day.up.plugin

import android.annotation.SuppressLint
import android.app.Application
import android.app.Instrumentation

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2021/1/28.
 * PS: Not easy to write code, please indicate.
 */
object HookStartActivityUtils {

    @SuppressLint("PrivateApi")
    fun hookStartActivity(application: Application) {
        try {
            val activityThreadClass=Class.forName("android.app.ActivityThread")
            val activityThreadField=activityThreadClass.getDeclaredField("sCurrentActivityThread")
            activityThreadField.isAccessible = true

            //获取ActivityThread对象sCurrentActivityThread
            val activityThread=activityThreadField.get(null)

            val instrumentationField= activityThreadClass.getDeclaredField("mInstrumentation")
            instrumentationField.isAccessible = true
            //从sCurrentActivityThread中获取成员变量mInstrumentation
            val instrumentation= instrumentationField.get(activityThread) as Instrumentation

            //创建代理对象InstrumentationProxy
            val proxy= InstrumentationProxy(instrumentation, application.packageManager)
            //将sCurrentActivityThread中成员变量mInstrumentation替换成代理类InstrumentationProxy
            instrumentationField.set(activityThread, proxy)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }
}