package com.verba.mobile

import android.app.Application
import com.verba.mobile.auth.AuthRepository
import com.verba.mobile.data.AllowedUsersRepository
import com.verba.mobile.data.FoldersRepository
import com.verba.mobile.data.api.LessonsApi
import com.verba.mobile.data.api.RunsApi
import com.verba.mobile.data.api.VerbaApiClient

class VerbaApp : Application() {
    val authRepository by lazy { AuthRepository() }
    val allowedUsersRepository by lazy { AllowedUsersRepository() }
    val foldersRepository by lazy { FoldersRepository() }
    val apiClient by lazy { VerbaApiClient() }
    val lessonsApi by lazy { LessonsApi(apiClient) }
    val runsApi by lazy { RunsApi(apiClient) }
}
