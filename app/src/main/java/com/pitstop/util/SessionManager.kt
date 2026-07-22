package com.pitstop.util

import android.content.Context

/**
 * Menyimpan sesi login sederhana (username & role) memakai SharedPreferences.
 */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun saveSession(username: String, role: String) {
        prefs.edit().putString(KEY_USERNAME, username).putString(KEY_ROLE, role).apply()
    }

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""
    fun getRole(): String = prefs.getString(KEY_ROLE, "") ?: ""

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
    }
}
