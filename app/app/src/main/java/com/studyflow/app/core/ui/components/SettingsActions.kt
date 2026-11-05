package com.studyflow.app.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.core.i18n.AppLanguage
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.settings.AppTheme
import com.studyflow.app.core.settings.LocalAppSettings

@Composable
fun SettingsActions(
    modifier: Modifier = Modifier,
    profileName: String? = null,
    onProfile: (() -> Unit)? = null,
) {
    val settings = LocalAppSettings.current
    val language by settings.language.collectAsStateWithLifecycle()
    val theme by settings.theme.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = settings::toggleLanguage) {
            Text(
                text = if (language == AppLanguage.PT_BR) "EN" else "PT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = settings::toggleTheme) {
            Icon(
                imageVector = if (theme == AppTheme.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = s[if (theme == AppTheme.DARK) "themeLightAction" else "themeDarkAction"],
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (onProfile != null) {
            IconButton(onClick = onProfile) {
                InitialAvatar(name = profileName.orEmpty(), size = 30.dp)
            }
        }
    }
}
