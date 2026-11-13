package com.studyflow.app.core.links

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.studyflow.app.BuildConfig

// As páginas de conta são servidas pelo mesmo host da API (nginx), por isso derivam do baseUrl.
fun siteUrl(path: String): String =
    BuildConfig.API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')

val resetPasswordUrl: String get() = siteUrl("user/reset-password")
val privacyPolicyUrl: String get() = siteUrl("app/privacidade/")

fun openExternal(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
