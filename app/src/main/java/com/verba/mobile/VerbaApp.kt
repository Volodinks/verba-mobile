package com.verba.mobile

import android.app.Application
import com.verba.mobile.auth.AuthRepository
import com.verba.mobile.data.AllowedUsersRepository
import com.verba.mobile.data.LessonsRepository

class VerbaApp : Application() {
    val authRepository by lazy { AuthRepository() }
    val allowedUsersRepository by lazy { AllowedUsersRepository() }
    val lessonsRepository by lazy { LessonsRepository() }
}
