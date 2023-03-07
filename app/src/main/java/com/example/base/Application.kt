package com.example.base

import android.util.Log
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Amplify

class Application : android.app.Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            Amplify.configure(applicationContext)
            Log.i("Application", "Initialized Amplify")
        } catch (error: AmplifyException) {
            Log.e("Application", "Could not initialize Amplify", error)
        }
    }
}