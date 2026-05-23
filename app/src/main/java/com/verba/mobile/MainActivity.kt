package com.verba.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.verba.mobile.ui.accessdenied.AccessDeniedScreen
import com.verba.mobile.ui.app.AppState
import com.verba.mobile.ui.app.AppViewModel
import com.verba.mobile.ui.lessons.LessonDetailScreen
import com.verba.mobile.ui.lessons.LessonListScreen
import com.verba.mobile.ui.login.LoginScreen
import com.verba.mobile.ui.navigation.Routes
import com.verba.mobile.ui.theme.VerbaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VerbaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot(appViewModel: AppViewModel = viewModel()) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (val s = state) {
        AppState.Initializing,
        AppState.CheckingAccess,
        -> CenteredLoading()

        AppState.NeedsLogin -> LoginScreen()

        is AppState.AccessDenied -> AccessDeniedScreen(
            email = s.email,
            onSignOut = { appViewModel.signOut(context) },
        )

        is AppState.AccessError -> AccessDeniedScreen(
            email = s.email,
            onSignOut = { appViewModel.signOut(context) },
        )

        is AppState.Authorized -> AuthorizedGraph(
            onSignOut = { appViewModel.signOut(context) },
        )
    }
}

@Composable
private fun AuthorizedGraph(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.LESSON_LIST,
    ) {
        composable(Routes.LESSON_LIST) {
            LessonListScreen(
                onOpenLesson = { id -> navController.navigate(Routes.lessonDetail(id)) },
                onSignOut = onSignOut,
            )
        }
        composable(
            route = Routes.LESSON_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_LESSON_ID) { type = NavType.StringType }),
        ) {
            LessonDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun CenteredLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
