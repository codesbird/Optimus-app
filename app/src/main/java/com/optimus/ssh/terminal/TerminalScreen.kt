package com.optimus.ssh.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optimus.ssh.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var promptPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Status Dot
                            val dotColor = if (state.isConnected) SuccessGreen else if (state.isConnecting) WarningYellow else ErrorRed
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                        state.host?.let { host ->
                            Text(
                                text = "${host.username}@${host.hostname}:${host.port}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = PrimaryAccent
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (state.isConnected) {
                                viewModel.disconnect()
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (state.isConnected) Icons.Default.PowerSettingsNew else Icons.Default.Close,
                            contentDescription = "Disconnect",
                            tint = if (state.isConnected) ErrorRed else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            if (state.isConnecting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundDark),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryAccent)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting to SSH server…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        state.host?.let { host ->
                            Text(
                                text = "${host.hostname}:${host.port}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else if (!state.isConnected && state.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundDark)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Connection Failed",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onBack) {
                                    Text("Back")
                                }
                                Button(
                                    onClick = { viewModel.connect() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                                ) {
                                    Text("Retry", color = BackgroundDark)
                                }
                            }
                        }
                    }
                }
            } else {
                // Interactive Terminal Surface
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    TerminalView(
                        emulator = viewModel.emulator,
                        onSendInput = { viewModel.sendInput(it) },
                        onResizePty = { cols, rows -> viewModel.resizePty(cols, rows) }
                    )
                }

                // Mobile Developer Keyboard Accessory Bar
                TerminalKeyboardBar(
                    onSendKey = { viewModel.sendInput(it) }
                )
            }
        }
    }

    // Password Prompt Dialog (if host has no saved password)
    if (state.needsPasswordInput) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text("SSH Password Required") },
            text = {
                Column {
                    Text(
                        text = "Enter password for ${state.host?.username}@${state.host?.hostname}:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = promptPassword,
                        onValueChange = { promptPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.connect(promptPassword)
                        promptPassword = ""
                    },
                    enabled = promptPassword.isNotEmpty()
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = onBack) {
                    Text("Cancel")
                }
            }
        )
    }
}
