package com.studyflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.i18n.Strings
import com.studyflow.app.core.settings.AppTheme
import com.studyflow.app.core.settings.LocalAppSettings
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.di.AppContainer
import com.studyflow.app.navigation.StudyFlowApp
import com.studyflow.app.ui.LocalContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container: AppContainer = (application as StudyFlowApplication).container
        setContent {
            val settings = container.settings
            val theme by settings.theme.collectAsStateWithLifecycle()
            val language by settings.language.collectAsStateWithLifecycle()

            StudyFlowTheme(darkTheme = theme == AppTheme.DARK) {
                CompositionLocalProvider(
                    LocalStrings provides Strings(language),
                    LocalAppSettings provides settings,
                    LocalContainer provides container,
                ) {
                    StudyFlowApp()
                }
            }
        }
    }
}
