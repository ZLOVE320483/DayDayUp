package com.zlove.day.up.plugin;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2021/1/28.
 * PS: Not easy to write code, please indicate.
 */
public class InstrumentationProxy extends Instrumentation {

    private static final String REQUEST_TARGET_INTENT_NAME = "request_target_intent_name";

    private final Instrumentation mInstrumentation;
    private final PackageManager mPackageManager;

    public InstrumentationProxy(Instrumentation instrumentation, PackageManager packageManager) {
        this.mInstrumentation = instrumentation;
        this.mPackageManager = packageManager;
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {

        List<ResolveInfo> resolveInfo = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            resolveInfo = mPackageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        } else {
            resolveInfo = mPackageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        }
        //判断启动的插件Activity是否在AndroidManifest.xml中注册过
        if (resolveInfo.size() == 0) {
            //保存目标插件
            intent.putExtra(REQUEST_TARGET_INTENT_NAME, intent.getComponent().getClassName());
            //设置为占坑Activity
            intent.setClassName(who, PlaceHolderActivity.class.getName());
        }

        try {
            Method execStartActivity = Instrumentation.class.getDeclaredMethod("execStartActivity",
                    Context.class, IBinder.class, IBinder.class, Activity.class,
                    Intent.class, int.class, Bundle.class);
            return (ActivityResult) execStartActivity.invoke(mInstrumentation, who, contextThread, token, target, intent, requestCode, options);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        String intentName = intent.getStringExtra(REQUEST_TARGET_INTENT_NAME);
        if (!TextUtils.isEmpty(intentName)) {
            Activity activity = super.newActivity(cl, intentName, intent);
            try {
                Class<?> contextClass = ContextThemeWrapper.class;
                Field field = contextClass.getDeclaredField("mResources");
                field.setAccessible(true);
                field.set(activity, PluginManager.getInstance().initPluginResource());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return activity;
        }
        return super.newActivity(cl, className, intent);
    }

}
