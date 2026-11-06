package com.autosign.cpdaily

import android.app.Application
import android.util.Log

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 全局崩溃异常捕获，防止 App 闪退
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("s1nger_Crash", "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
        }
    }
}
