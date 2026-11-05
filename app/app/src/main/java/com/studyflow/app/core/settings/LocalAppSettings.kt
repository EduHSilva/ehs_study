package com.studyflow.app.core.settings

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppSettings = staticCompositionLocalOf<AppSettings> {
    error("AppSettings não fornecido")
}
