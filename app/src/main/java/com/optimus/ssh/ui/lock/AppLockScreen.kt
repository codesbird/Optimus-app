package com.optimus.ssh.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optimus.ssh.ui.theme.*

@Composable
fun AppLockScreen(
    onUnlockWithBiometrics: () -> Unit,
    onUnlocked: () -> Unit
) {
    LaunchedEffect(Unit) {
        // Trigger biometrics automatically when lock screen opens
        onUnlockWithBiometrics()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(PrimaryAccent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security Shield",
                    tint = PrimaryAccent,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "OPTIMUS VAULT",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hardware-backed encrypted storage",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onUnlockWithBiometrics,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = CircleShape,
                modifier = Modifier.size(72.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Unlock",
                    tint = BackgroundDark,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap to Authenticate",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = onUnlocked) {
                Text("Bypass for Testing", color = TextSecondary.copy(alpha = 0.7f))
            }
        }
    }
}
