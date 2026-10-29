package com.marco90.mlgpstest.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data class Available(val tag: String, val url: String, val sizeBytes: Long, val notes: String) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class Ready(val tag: String) : UpdateState
    data class Error(val message: String) : UpdateState
}

/** Controlla le Release di GitHub, scarica l'APK più recente e avvia l'installazione. */
class Updater(private val context: Context) {

    companion object {
        const val REPO = "marco90-cyber/MLGPSTest"
        private const val UA = "MLGPSTest-updater"
    }

    private val apkFile: File = File(File(context.cacheDir, "updates").also { it.mkdirs() }, "MLGPSTest.apk")

    fun currentVersionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    @Suppress("DEPRECATION")
    fun currentVersionCode(): Long = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    } catch (e: Exception) {
        0L
    }

    suspend fun check(): UpdateState = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", UA)
            }
            if (conn.responseCode == 404) return@withContext UpdateState.Error("Nessuna versione pubblicata")
            if (conn.responseCode != 200) return@withContext UpdateState.Error("Errore GitHub (${conn.responseCode})")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name")
            val remote = tag.substringAfterLast('.').toLongOrNull()
                ?: return@withContext UpdateState.Error("Versione non riconosciuta: $tag")
            val assets = json.getJSONArray("assets")
            var url: String? = null
            var size = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) {
                    url = a.getString("browser_download_url")
                    size = a.optLong("size")
                    break
                }
            }
            when {
                remote <= currentVersionCode() -> UpdateState.UpToDate(currentVersionName())
                url == null -> UpdateState.Error("La release $tag non contiene un APK")
                else -> UpdateState.Available(tag, url, size, json.optString("body"))
            }
        } catch (e: Exception) {
            UpdateState.Error("Controllo non riuscito: controlla la connessione")
        }
    }

    /** Scarica l'APK; [onProgress] riceve un valore 0..1 (o -1 se la dimensione è ignota). */
    suspend fun download(info: UpdateState.Available, onProgress: (Float) -> Unit): UpdateState =
        withContext(Dispatchers.IO) {
            try {
                apkFile.delete()
                val conn = (URL(info.url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", UA)
                }
                if (conn.responseCode != 200) return@withContext UpdateState.Error("Download non riuscito (${conn.responseCode})")
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.sizeBytes
                var done = 0L
                conn.inputStream.use { input ->
                    apkFile.outputStream().use { out ->
                        val buf = ByteArray(32 * 1024)
                        var lastReport = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            val now = System.currentTimeMillis()
                            if (now - lastReport > 150) {
                                lastReport = now
                                onProgress(if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else -1f)
                            }
                        }
                    }
                }
                if (total > 0 && done < total) {
                    apkFile.delete()
                    return@withContext UpdateState.Error("Download interrotto")
                }
                UpdateState.Ready(info.tag)
            } catch (e: Exception) {
                apkFile.delete()
                UpdateState.Error("Download non riuscito: controlla la connessione")
            }
        }

    /** True se l'app può installare APK; altrimenti apre la schermata di sistema per concedere il permesso. */
    fun ensureInstallPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        if (context.packageManager.canRequestPackageInstalls()) return true
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return false
    }

    fun install(): Boolean {
        if (!apkFile.exists()) return false
        if (!ensureInstallPermission()) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }
}
