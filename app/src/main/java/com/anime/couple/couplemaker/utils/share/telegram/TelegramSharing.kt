package com.anime.couple.couplemaker.utils.share.telegram

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.anime.couple.couplemaker.R


object TelegramSharing {

    fun importToTelegram(context: Context, uriList: List<Uri>) {
        if (uriList.isEmpty()) return

        val list = ArrayList(uriList)

        list.forEach { uri ->
            try {
                context.grantUriPermission(
                    "org.telegram.messenger",
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { }
        }

        val intent = Intent("org.telegram.messenger.CREATE_STICKER_PACK").apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, list)
            putExtra("IMPORTER", context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "image/*"
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.no_app_found_to_handle_this_action),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}