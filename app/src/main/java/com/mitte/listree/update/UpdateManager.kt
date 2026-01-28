package com.mitte.listree.update

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import com.google.gson.Gson
import com.mitte.listree.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object UpdateManager {

    private const val UPDATE_URL = "https://raw.githubusercontent.com/Mitte76/Listree/main/update.json"

    suspend fun checkForUpdate(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val json = URL(UPDATE_URL).readText()
                val updateInfo = Gson().fromJson(json, UpdateInfo::class.java)

                if (updateInfo.versionCode > getCurrentVersionCode(context)) {
                    downloadUpdate(context, updateInfo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }
    }

    private fun downloadUpdate(context: Context, updateInfo: UpdateInfo) {
        val request = DownloadManager.Request(Uri.parse(updateInfo.apkUrl))
            .setTitle("Updating LisTree")
            .setDescription("Downloading the latest version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app-release.apk")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
}
