package com.verba.mobile.ui.accessdenied

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.verba.mobile.R

@Composable
fun AccessDeniedScreen(
    email: String,
    onSignOut: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 400.dp),
        ) {
            Text(
                text = stringResource(R.string.access_denied_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.access_denied_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (email.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.access_denied_signed_in_as),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = onSignOut) {
                Text(stringResource(R.string.sign_out))
            }
        }
    }
}
