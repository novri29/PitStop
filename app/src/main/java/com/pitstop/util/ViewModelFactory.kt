package com.pitstop.util

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pitstop.repository.AppRepository

/**
 * Factory generik: semua ViewModel di app ini menerima AppRepository di constructor-nya.
 */
class ViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val repository = AppRepository(context.applicationContext)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(AppRepository::class.java).newInstance(repository)
    }
}
