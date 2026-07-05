package com.wendao.run.core.data

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionStore(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "paoxiu_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getOrCreateDeviceId(context: Context): String {
        val cached = prefs.getString(KEY_DEVICE_ID, null)
        if (!cached.isNullOrBlank()) return cached
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val deviceId = if (androidId.isNullOrBlank()) "guest-${java.util.UUID.randomUUID()}" else "android-$androidId"
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }

    fun getDeviceId(context: Context): String = getOrCreateDeviceId(context)

    fun saveSession(accessToken: String, refreshToken: String, userId: Long) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

    fun getUserId(): Long? {
        val id = prefs.getLong(KEY_USER_ID, -1L)
        return if (id <= 0L) null else id
    }

    fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_USER_ID)
            .apply()
    }

    /** @deprecated 使用 clearSession()，保留 deviceId 与引导状态 */
    fun clear() {
        clearSession()
    }

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_STORY_INTRO_SEEN = "story_intro_seen"
    }

    fun hasSeenStoryIntro(): Boolean = prefs.getBoolean(KEY_STORY_INTRO_SEEN, false)

    fun setStoryIntroSeen() {
        prefs.edit().putBoolean(KEY_STORY_INTRO_SEEN, true).apply()
    }
}
