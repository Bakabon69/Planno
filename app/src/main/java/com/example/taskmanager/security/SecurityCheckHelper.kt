package com.example.taskmanager.security

import android.os.Build
import java.io.File

/**
 * Validates runtime environment integrity to guard against debugging,
 * reverse-engineering hooks, and root compromise.
 */
object SecurityCheckHelper {

    /**
     * Checks if the device appears rooted or modified with superuser binaries.
     */
    fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su-backup"
        )
        return paths.any { File(it).exists() } || checkBuildTags()
    }

    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
}
