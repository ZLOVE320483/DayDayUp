package com.zlove.day.gradle.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2021/1/18.
 * PS: Not easy to write code, please indicate.
 */
class MyTask extends DefaultTask {

    @TaskAction
    void action() {
        System.out.println("my task run");
    }
}
