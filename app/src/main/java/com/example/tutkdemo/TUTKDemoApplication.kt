package com.example.tutkdemo

import android.app.Application
import timber.log.Timber.*
import timber.log.Timber.Forest.plant


class TUTKDemoApplication: Application() {
    val avProvider = AVProvider()
    init {
        plant(DebugTree())
    }
}