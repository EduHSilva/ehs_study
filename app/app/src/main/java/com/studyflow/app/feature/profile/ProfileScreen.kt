package com.studyflow.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.links.openExternal
import com.studyflow.app.core.links.privacyPolicyUrl
import com.studyflow.app.core.ui.components.ContentCard
import com.studyflow.app.core.ui.components.ErrorBanner
import com.studyflow.app.core.ui.components.FieldLabel
import com.studyflow.app.core.ui.components.InitialAvatar
import com.studyflow.app.core.ui.components.Subtitle
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.ui.appViewModel

@Composable
fun ProfileScreen() {
    val vm: ProfileViewModel = appViewModel { ProfileViewModel(it.authRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val context = LocalContext.current

    val user = session?.user
    var name by rememberSaveable(user?.name) { mutableStateOf(user?.name.orEmpty()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Subtitle(s["profileSubtitle"]) }

        item {
            ContentCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    InitialAvatar(name = user?.name.orEmpty(), size = 64.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            user?.name.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            user?.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            ContentCard {
                FieldLabel(s["profileNameLabel"])
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(s["usernamePlaceholder"]) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(14.dp))
                FieldLabel(s["profileEmailLabel"])
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = user?.email.orEmpty(),
                    onValueChange = {},
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(14.dp))
                FieldLabel(s["profileIdLabel"])
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = user?.id.orEmpty(),
                    onValueChange = {},
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.errorKey != null) {
                    Spacer(Modifier.height(14.dp))
                    ErrorBanner(message = s[state.errorKey!!])
                }
                if (state.saved) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = s["profileSaveSuccess"],
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudyFlowTheme.colors.successSoft, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudyFlowTheme.colors.success,
                    )
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { vm.updateName(name) },
                    enabled = !state.pending,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (state.pending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.pending) s["profileSaving"] else s["profileSave"])
                }
            }
        }

        item {
            ContentCard {
                TextButton(
                    onClick = { openExternal(context, privacyPolicyUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Filled.PrivacyTip,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(s["privacyPolicy"])
                }
            }
        }
    }
}
