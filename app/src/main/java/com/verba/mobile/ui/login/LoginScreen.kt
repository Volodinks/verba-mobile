package com.verba.mobile.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verba.mobile.R

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        ) {
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(48.dp))

            if (state is LoginUiState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(onClick = { viewModel.signIn(context) }) {
                    Text(stringResource(R.string.login_button))
                }
            }

            Spacer(Modifier.height(16.dp))

            val errorMessage: Int? = when (state) {
                LoginUiState.Cancelled -> R.string.login_cancelled
                LoginUiState.NoGoogleAccount -> R.string.login_no_google_account
                LoginUiState.NetworkError -> R.string.login_network_error
                else -> null
            }
            if (errorMessage != null) {
                Text(
                    text = stringResource(errorMessage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
