package com.pitstop.helper

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val pref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = pref.edit()

    companion object {
        private const val PREF_NAME = "PitstopSession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
    }

    // Menyimpan data sesi saat pengguna berhasil login
    fun saveSession(username: String, role: String) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USERNAME, username)
        editor.putString(KEY_ROLE, role)
        editor.apply()
    }

    // Mengecek apakah pengguna sudah login
    fun isLoggedIn(): Boolean {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Mengambil data username (mengembalikan string kosong jika null)
    fun getUsername(): String {
        return pref.getString(KEY_USERNAME, "") ?: ""
    }

    // Mengambil data role (mengembalikan string kosong jika null)
    fun getRole(): String {
        return pref.getString(KEY_ROLE, "") ?: ""
    }

    // Menghapus sesi saat pengguna menekan tombol Logout
    fun clearSession() {
        editor.clear()
        editor.apply()
    }
}