package com.kogen.androidarc.demo

import android.app.Application
import com.kogen.androidarc.demo.di.setApplicationContext

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setApplicationContext(this)
    }
}
