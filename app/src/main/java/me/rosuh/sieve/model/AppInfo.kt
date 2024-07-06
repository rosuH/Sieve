package me.rosuh.sieve.model

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable

@Immutable
data class AppList(
    val list: List<AppInfo>
) : List<AppInfo> by list {
    companion object {
        val empty = AppList(emptyList())
    }
}

@Immutable
data class AppInfo(
    val appName: String,
    val icon: Int,
    val packageName: String,
    val isUserApp: Boolean,
) {
    lateinit var applicationInfo: ApplicationInfo

    companion object {
        fun fromApplicationInfo(
            applicationInfo: ApplicationInfo,
            packageManager: PackageManager
        ): AppInfo {
            return AppInfo(
                applicationInfo.loadLabel(packageManager).toString(),
                applicationInfo.icon,
                applicationInfo.packageName,
                applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
            ).also {
                it.applicationInfo = applicationInfo
            }
        }
    }
}
