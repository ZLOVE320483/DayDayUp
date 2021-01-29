package com.zlove.day.up.plugin;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2021/1/27.
 * PS: Not easy to write code, please indicate.
 */
public class PluginManager {
    private PluginManager() {

    }
    private volatile static PluginManager instance;
    private Context context;
    private String pluginPath;

    public static PluginManager getInstance() {
        if (instance == null) {
            synchronized (PluginManager.class) {
                if (instance == null) {
                    instance = new PluginManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context, String pluginPath) {
        this.context = context;
        this.pluginPath = pluginPath;
    }

    public void insertDex(ClassLoader origin)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        File optimizeFile = context.getFileStreamPath("plugin");
        if (!optimizeFile.exists()) {
            optimizeFile.mkdirs();
        }

        System.out.println("optimizeFile --- " + optimizeFile.getAbsolutePath());

        // 将pluginPath 传入DexClassLoader 用来加载该路径下的插件的类
        //在构造插件的ClassLoader时会传入主工程的ClassLoader作为父加载器，所以插件是可以直接可以通过类名引用主工程的类。
        DexClassLoader pluginClassLoader = new DexClassLoader(pluginPath, optimizeFile.getAbsolutePath(), null, origin);
        Class<?> baseLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        // 拿到BaseDexClassLoader中的pathList变量
        Field pathListField = baseLoaderClass.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        // 拿到pathList 值
        Object pluginDexPathList = pathListField.get(pluginClassLoader);

        // 取到pathList 中的dexElements中的值
        Class<?> dexPathListClass = Class.forName("dalvik.system.DexPathList");
        Field dexElementFiled = dexPathListClass.getDeclaredField("dexElements");
        dexElementFiled.setAccessible(true);
        Object dexElements = dexElementFiled.get(pluginDexPathList);

        // 取到宿主的ClassLoader中的pathList值
        Class<?> originbaseLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        Field originpathListField = originbaseLoaderClass.getDeclaredField("pathList");
        originpathListField.setAccessible(true);
        Object originDexPathList = originpathListField.get(origin);

        // 拿到宿主中的pathList的dexElements
        Class<?> origindexPathListClass = Class.forName("dalvik.system.DexPathList");
        Field origindexElementFiled = origindexPathListClass.getDeclaredField("dexElements");
        origindexElementFiled.setAccessible(true);
        Object origindexElements = origindexElementFiled.get(originDexPathList);

        // 合并宿主和插件中的dexElements
        Object allDexElements = combineArray(dexElements, origindexElements);

        // 将合并后的dexElements 设置到dexElements 变量中
        Class<?> alldexPathListClass = Class.forName("dalvik.system.DexPathList");
        Field alldexElementFiled = alldexPathListClass.getDeclaredField("dexElements");
        alldexElementFiled.setAccessible(true);
        alldexElementFiled.set(originDexPathList, allDexElements);
    }

    public Resources initPluginResource()
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> assetManagerClass = AssetManager.class;
        Object assetManager = assetManagerClass.newInstance();
        Method addAssetPathMethod = assetManagerClass.getMethod("addAssetPath", String.class);
        // 调用AssetManager 中的addAssetPath 方法，将插件路径传入
        addAssetPathMethod.invoke(assetManager, pluginPath);
        // 构造插件的Resource对象
        return new Resources((AssetManager) assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
    }

    //合并数组
    private static Object combineArray(Object firstArray, Object secondArray) {
        Class<?> localClass = firstArray.getClass().getComponentType();
        int firstArrayLength = Array.getLength(firstArray);
        int secondArrayLength = Array.getLength(secondArray);
        Object result = Array.newInstance(localClass, firstArrayLength + secondArrayLength);
        System.arraycopy(firstArray, 0, result, 0, firstArrayLength);
        System.arraycopy(secondArray, 0, result, firstArrayLength, secondArrayLength);
        return result;
    }


}
