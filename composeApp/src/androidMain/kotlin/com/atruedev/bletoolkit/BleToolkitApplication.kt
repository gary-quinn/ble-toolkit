package com.atruedev.bletoolkit

import android.app.Application
import com.atruedev.kmpble.KmpBle

class BleToolkitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KmpBle.init(this)
    }
}
