package com.mitte.listree.update

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.net.toUri
import com.google.gson.Gson
import com.mitte.listree.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object UpdateManager {

    private const val UPDATE_URL = "https://raw.githubusercontent.com/Mitte76/Listree/master/update.json"

    suspend fun checkForUpdate(context: Context): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val json = URL(UPDATE_URL).readText()
                val updateInfo = Gson().fromJson(json, UpdateInfo::class.java)
                val currentVersionCode = getCurrentVersionCode(context)

                if (updateInfo.versionCode > currentVersionCode) {
                    return@withContext updateInfo
                } else {
                    return@withContext null
                }
            } catch (_: Exception) {
                return@withContext null
            }
        }
    }

    fun startUpdateDownload(context: Context, updateInfo: UpdateInfo) {
        val request = DownloadManager.Request(updateInfo.apkUrl.toUri())
            .setTitle("Updating LisTree")
            .setDescription("Downloading the latest version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app-release.apk")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (_: PackageManager.NameNotFoundException) {
            -1
        }
    }
}
