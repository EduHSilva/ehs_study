package com.studyflow.app.feature.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.ui.components.ContentCard
import com.studyflow.app.core.ui.components.ErrorBanner
import com.studyflow.app.core.ui.components.SettingsActions
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.ui.LocalContainer
import com.studyflow.app.ui.appViewModel
import kotlinx.coroutines.launch

private enum class AuthMode { LOGIN, REGISTER }

@Composable
fun AuthScreen() {
    val container = LocalContainer.current
    val vm: AuthViewModel = appViewModel { AuthViewModel(it.authRepository, it.settings) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    var mode by rememberSaveable { mutableStateOf(AuthMode.LOGIN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? Activity

    val submit = {
        focusManager.clearFocus()
        if (mode == AuthMode.LOGIN) vm.login(email, password)
        else vm.register(email, username, password)
    }

    val switchMode = { next: AuthMode ->
        mode = next
        showPassword = false
        vm.clearError()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark()
                SettingsActions()
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = s["landingTitle"],
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = s["landingSubtitle"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                listOf(s["landingPointTimer"], s["landingPointNotes"], s["landingPointTasks"]).forEach { point ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(StudyFlowTheme.colors.successSoft, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = StudyFlowTheme.colors.success,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                        Text(point, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (mode == AuthMode.REGISTER && state.registerSuccess) {
                ContentCard {
                    Text(s["registerSuccessTitle"], style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s["registerSuccessText"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            vm.resetRegisterSuccess()
                            switchMode(AuthMode.LOGIN)
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(s["loginNow"])
                    }
                }
            } else {
                ContentCard {
                    Text(
                        if (mode == AuthMode.LOGIN) s["loginTitle"] else s["registerTitle"],
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (mode == AuthMode.LOGIN) s["loginSubtitle"] else s["registerSubtitle"],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (mode == AuthMode.REGISTER) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(s["usernameLabel"]) },
                            placeholder = { Text(s["usernamePlaceholder"]) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(s["emailLabel"]) },
                        placeholder = { Text(s["emailPlaceholder"]) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(s["passwordLabel"]) },
                        placeholder = { Text("••••••••") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = s[if (showPassword) "hidePassword" else "showPassword"],
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val errorKey = if (mode == AuthMode.LOGIN) state.errorKey else state.registerErrorKey
                    if (errorKey != null) {
                        Spacer(Modifier.height(14.dp))
                        ErrorBanner(message = s[errorKey])
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = submit,
                        enabled = !(if (mode == AuthMode.LOGIN) state.pending else state.registerPending),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (if (mode == AuthMode.LOGIN) state.pending else state.registerPending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            when {
                                mode == AuthMode.LOGIN && state.pending -> s["loggingIn"]
                                mode == AuthMode.LOGIN -> s["loginAction"]
                                state.registerPending -> s["registering"]
                                else -> s["registerAction"]
                            },
                        )
                    }

                    if (mode == AuthMode.LOGIN && container.googleLoginEnabled && activity != null) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                s["or"],
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = StudyFlowTheme.colors.subtle,
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val token = runCatching {
                                        GoogleSignIn.requestIdToken(activity, container.googleWebClientId)
                                    }.getOrNull()
                                    if (token != null) vm.loginWithGoogle(token) else vm.reportGoogleFailure()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("G", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(s["googleLoginAction"])
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            if (mode == AuthMode.LOGIN) s["needAccount"] else s["alreadyHaveAccount"],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { switchMode(if (mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN) }) {
                            Text(
                                if (mode == AuthMode.LOGIN) s["goToRegister"] else s["goToLogin"],
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun BrandMark(dark: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        Text(
            text = "StudyFlow",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (dark) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onBackground,
        )
    }
}
