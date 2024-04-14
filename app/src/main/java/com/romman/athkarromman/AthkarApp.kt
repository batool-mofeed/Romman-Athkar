package com.romman.athkarromman

import android.app.Application
import com.romman.athkarromman.utils.Prefs

/**
 * Created By Batool Mofeed - 09/04/2024.
 **/

class AthkarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}