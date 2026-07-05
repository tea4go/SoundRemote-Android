package io.github.soundremote.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.soundremote.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 应用内更新服务：查询 GitHub / Gitee 最新 Release，解析 APK 资产名，
 * 与当前 versionCode 对比；有更新时下载 APK 到 cache，触发系统安装器。
 *
 * APK 命名约定：`SoundRemote-<versionName>-<versionCode>.apk`
 * 该命名由 scripts/windows/build_android_bywin.ps1 保证。
 *
 * 无需引入 OkHttp/Retrofit，使用标准 HttpURLConnection + kotlinx.serialization。
 */
object UpdateService {

    private const val GITHUB_OWNER = "tea4go"
    private const val GITHUB_REPO = "SoundRemote-Android"
    private const val GITEE_OWNER = "tea4go"
    private const val GITEE_REPO = "SoundRemote-Android"

    private const val REQUEST_TIMEOUT_MS = 10_000

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 检查更新。
     * @param source 使用哪个 API 源（gitee/github），一般由用户设置决定
     * @return 检查结果；网络异常或格式不符时返回 [UpdateInfo.NONE]
     */
    suspend fun checkForUpdate(source: UpdateSource): UpdateInfo = withContext(Dispatchers.IO) {
        val apiUrl = when (source) {
            UpdateSource.GITEE -> "https://gitee.com/api/v5/repos/$GITEE_OWNER/$GITEE_REPO/releases/latest"
            UpdateSource.GITHUB -> "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        }
        val body = runCatching { httpGet(apiUrl) }
            .onFailure { Timber.w(it, "Fetch $apiUrl failed") }
            .getOrNull() ?: return@withContext UpdateInfo.NONE

        val release = runCatching { json.decodeFromString<GhRelease>(body) }
            .onFailure { Timber.w(it, "Parse release failed") }
            .getOrNull() ?: return@withContext UpdateInfo.NONE

        val apkAsset = release.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return@withContext UpdateInfo.NONE
        val parsed = parseApkName(apkAsset.name) ?: return@withContext UpdateInfo.NONE

        UpdateInfo(
            hasUpdate = parsed.second > BuildConfig.VERSION_CODE,
            versionName = parsed.first,
            versionCode = parsed.second,
            downloadUrl = apkAsset.browser_download_url,
            releaseNotes = release.body.orEmpty(),
        )
    }

    /**
     * 下载 APK 到 cache 目录，返回本地文件；调用方随后应触发系统安装器。
     */
    suspend fun downloadApk(context: Context, url: String, versionCode: Int): File =
        withContext(Dispatchers.IO) {
            val dstDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val dst = File(dstDir, "SoundRemote-update-$versionCode.apk")
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = REQUEST_TIMEOUT_MS
                readTimeout = REQUEST_TIMEOUT_MS
                instanceFollowRedirects = true
            }
            try {
                conn.inputStream.use { input ->
                    dst.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } finally {
                conn.disconnect()
            }
            dst
        }

    /**
     * 用系统安装器打开 APK。需在 AndroidManifest 声明 FileProvider 和
     * REQUEST_INSTALL_PACKAGES 权限；用户还需在系统设置里授予"安装未知应用"。
     */
    fun openInstaller(context: Context, apk: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    /**
     * 解析 APK 名 `SoundRemote-<versionName>-<versionCode>.apk`。
     * @return Pair<versionName, versionCode>；不匹配返回 null。
     */
    private fun parseApkName(name: String): Pair<String, Int>? {
        val m = Regex("""^SoundRemote-(.+)-(\d+)\.apk$""", RegexOption.IGNORE_CASE).find(name)
            ?: return null
        val versionCode = m.groupValues[2].toIntOrNull() ?: return null
        return m.groupValues[1] to versionCode
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = REQUEST_TIMEOUT_MS
            readTimeout = REQUEST_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SoundRemote-Android/${BuildConfig.VERSION_NAME}")
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // GitHub/Gitee Release API 的最小字段解析
    @Serializable
    private data class GhRelease(
        val body: String? = null,
        val assets: List<GhAsset>? = null,
    )

    @Serializable
    private data class GhAsset(
        val name: String,
        @SerialName("browser_download_url") val browser_download_url: String,
    )
}
