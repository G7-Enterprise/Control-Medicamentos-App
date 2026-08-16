package com.carlos.controlmedicamentos

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Lee la configuraciÃ³n de actualizaciones sin bloquear la interfaz.
 * Los valores locales mantienen la aplicaciÃ³n operativa si no hay conexiÃ³n.
 */
data class AppUpdateConfig(
    val latestVersion: String,
    val latestVersionCode: Int,
    val updateUrl: String,
    val forceUpdate: Boolean
)

data class AppUpdateCheck(
    val config: AppUpdateConfig,
    val updateAvailable: Boolean,
    val forceUpdate: Boolean
)

object AppUpdateRemoteConfig {
    private const val TAG = "AppUpdateRemoteConfig"

    const val KEY_LATEST_VERSION = "ultima_version_disponible"
    const val KEY_LATEST_VERSION_CODE = "ultima_version_code"
    const val KEY_UPDATE_URL = "url_actualizacion"
    const val KEY_FORCE_UPDATE = "forzar_actualizacion"

    private const val FETCH_INTERVAL_SECONDS = 12 * 60 * 60L
    private const val GITHUB_REPO = "G7-Enterprise/Control-Medicamentos-App"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private const val GITHUB_DOWNLOAD_BASE = "https://github.com/$GITHUB_REPO/releases/download"

    private val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchAndCheck(
        installedVersion: String = BuildConfig.VERSION_NAME,
        installedVersionCode: Int = BuildConfig.VERSION_CODE,
        forceFetch: Boolean = false
    ): AppUpdateCheck {
        // First try to get version from GitHub releases
        val githubUpdate = fetchGitHubRelease()
        
        val defaults = mapOf<String, Any>(
            KEY_LATEST_VERSION to (githubUpdate?.version ?: installedVersion),
            KEY_LATEST_VERSION_CODE to (githubUpdate?.versionCode ?: installedVersionCode),
            KEY_UPDATE_URL to (githubUpdate?.downloadUrl ?: ""),
            KEY_FORCE_UPDATE to false
        )

        try {
            remoteConfig.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(
                        if (forceFetch) 0 else FETCH_INTERVAL_SECONDS
                    )
                    .build()
            ).await()
            remoteConfig.setDefaultsAsync(defaults).await()
            remoteConfig.fetchAndActivate().await()
        } catch (exception: Exception) {
            // Remote Config conserva los valores activados o los defaults; no impedimos el inicio offline.
            Log.w(TAG, "No se pudo actualizar Remote Config; se usarÃ¡ la configuraciÃ³n local", exception)
        }

        val remoteVersion = remoteConfig.getString(KEY_LATEST_VERSION)
        val remoteVersionCode = remoteConfig.getLong(KEY_LATEST_VERSION_CODE).toInt().takeIf { it > 0 }
        val remoteUpdateUrl = remoteConfig.getString(KEY_UPDATE_URL)
        val config = AppUpdateConfig(
            latestVersion = githubUpdate?.version ?: remoteVersion.ifBlank { installedVersion },
            latestVersionCode = githubUpdate?.versionCode ?: remoteVersionCode ?: installedVersionCode,
            updateUrl = githubUpdate?.downloadUrl ?: remoteUpdateUrl,
            forceUpdate = remoteConfig.getBoolean(KEY_FORCE_UPDATE)
        )
        val updateAvailable = isVersionNewer(config.latestVersion, installedVersion)

        return AppUpdateCheck(
            config = config,
            updateAvailable = updateAvailable,
            forceUpdate = updateAvailable && config.forceUpdate
        )
    }

    private suspend fun fetchGitHubRelease(): GitHubReleaseInfo? {
        return try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "GitHub API request failed: ${response.code}")
                return null
            }
            
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            
            val tagName = json.optString("tag_name", "")
            val version = tagName.removePrefix("v")
            val assets = json.optJSONArray("assets")
            
            var downloadUrl = ""
            if (assets != null && assets.length() > 0) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }
            
            // If not found in assets, construct URL from tag name
            if (downloadUrl.isBlank() && tagName.isNotEmpty()) {
                downloadUrl = "$GITHUB_DOWNLOAD_BASE/$tagName/app-release_ambassador.apk"
            }
            
            val versionCode = extractVersionCode(version)
            
            GitHubReleaseInfo(
                version = version,
                versionCode = versionCode,
                downloadUrl = downloadUrl
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch GitHub release", e)
            null
        }
    }

    private fun extractVersionCode(version: String): Int {
        val parts = versionParts(version)
        return if (parts.size >= 3) {
            parts[0] * 10000 + parts[1] * 100 + parts[2]
        } else {
            0
        }
    }

    private data class GitHubReleaseInfo(
        val version: String,
        val versionCode: Int,
        val downloadUrl: String
    )

    internal fun isVersionNewer(remoteVersion: String, installedVersion: String): Boolean {
        val remoteParts = versionParts(remoteVersion)
        val installedParts = versionParts(installedVersion)
        val largestSize = maxOf(remoteParts.size, installedParts.size)

        for (index in 0 until largestSize) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val installedPart = installedParts.getOrElse(index) { 0 }
            if (remotePart != installedPart) return remotePart > installedPart
        }
        return false
    }

    private fun versionParts(version: String): List<Int> =
        version.trim()
            .split(Regex("[^0-9]+"))
            .filter { it.isNotEmpty() }
            .map { it.toIntOrNull() ?: 0 }
            .ifEmpty { listOf(0) }
}
