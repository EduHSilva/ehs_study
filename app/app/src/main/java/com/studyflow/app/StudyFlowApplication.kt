package com.studyflow.app

import android.app.Application
import com.studyflow.app.di.AppContainer

class StudyFlowApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
