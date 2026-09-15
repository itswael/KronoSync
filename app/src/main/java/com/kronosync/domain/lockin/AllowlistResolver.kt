package com.kronosync.domain.lockin

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.telecom.TelecomManager

/**
 * Camera and phone-dialer package names vary by OEM (Google Camera, Samsung Camera, etc.), so
 * rather than hardcoding a list, resolve *this device's* actual default apps at runtime via
 * PackageManager/TelecomManager — the same mechanism Android itself uses to launch them.
 */
object AllowlistResolver {

    fun allowedPackages(context: Context): Set<String> {
        val packages = mutableSetOf(context.packageName)
        resolveDefaultCamera(context)?.let { packages.add(it) }
        resolveDefaultDialer(context)?.let { packages.add(it) }
        return packages
    }

    private fun resolveDefaultCamera(context: Context): String? {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
    }

    private fun resolveDefaultDialer(context: Context): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.defaultDialerPackage
        } else {
            val intent = Intent(Intent.ACTION_DIAL)
            context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        }
    }
}
