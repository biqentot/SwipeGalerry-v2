package com.biqael.swipegallery.util

import android.content.Context

object Prefs {
    private const val PREF = "p"
    private const val KEY_KEPT = "kept"
    private const val KEY_ONBOARDING = "onboarding_done"
    private const val KEY_LAST_BUCKET = "last_bucket"
    private const val KEY_LAST_BUCKET_NAME = "last_bucket_name"

    fun getKept(context: Context): MutableSet<Long> {
        val s = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getStringSet(KEY_KEPT, emptySet()) ?: emptySet()
        return s.mapNotNull { it.toLongOrNull() }.toMutableSet()
    }

    fun saveKept(context: Context, kept: Set<Long>) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_KEPT, kept.map { it.toString() }.toSet()).apply()
    }

    fun isOnboardingDone(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING, false)

    fun setOnboardingDone(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING, true).apply()
    }

    fun setLastFolder(context: Context, bucketId: String?, bucketName: String?) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_BUCKET, bucketId)
            .putString(KEY_LAST_BUCKET_NAME, bucketName)
            .apply()
    }

    fun getLastFolder(context: Context): Pair<String?, String?> {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return Pair(p.getString(KEY_LAST_BUCKET, null), p.getString(KEY_LAST_BUCKET_NAME, null))
    }
}
