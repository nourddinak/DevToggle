package com.devtoggle.app.data

import com.devtoggle.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Updater {
    private const val REPO_OWNER = "nourddinak"
    private const val REPO_NAME = "DevToggle"
    private const val API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    /**
     * Checks for updates and returns a Pair of (new_version_name, release_url) if an update is available.
     * Returns null if no update is available or if an error occurs.
     */
    suspend fun checkForUpdates(): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                var latestVersion = json.getString("tag_name")
                val releaseUrl = json.getString("html_url")
                
                // Remove 'v' prefix if exists (e.g. "v1.0.1" -> "1.0.1")
                if (latestVersion.startsWith("v", ignoreCase = true)) {
                    latestVersion = latestVersion.substring(1)
                }

                val currentVersion = BuildConfig.VERSION_NAME

                if (isNewerVersion(currentVersion, latestVersion)) {
                    return@withContext Pair(latestVersion, releaseUrl)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until length) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
