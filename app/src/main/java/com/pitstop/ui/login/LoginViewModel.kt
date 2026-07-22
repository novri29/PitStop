package com.pitstop.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pitstop.save.entity.User
import com.pitstop.repository.AppRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AppRepository) : ViewModel() {

    fun login(username: String, password: String, onResult: (User?) -> Unit) {
        viewModelScope.launch {
            val user = repository.login(username, password)
            onResult(user)
        }
    }
}
