package com.optimus.ssh.ui.keys

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optimus.ssh.model.KeyAlgorithm
import com.optimus.ssh.model.KeyRecord
import com.optimus.ssh.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeysScreen(
    viewModel: KeysViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showFabMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = SecondaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SSH Keys",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            viewModel.openImportDialog()
                        },
                        containerColor = SurfaceVariantDark,
                        contentColor = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Key")
                    }

                    ExtendedFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            viewModel.openGenerateDialog()
                        },
                        containerColor = PrimaryAccent,
                        contentColor = BackgroundDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Key")
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = SecondaryAccent,
                    contentColor = BackgroundDark
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Key,
                        contentDescription = "Manage Keys"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            } else if (state.keys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No SSH Keys Stored",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generate modern Ed25519/RSA keys or import existing ones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.keys, key = { it.id }) { keyRecord ->
                        KeyCard(
                            keyRecord = keyRecord,
                            onCopyPublicKey = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("SSH Public Key", keyRecord.publicKey)
                                clipboard.setPrimaryClip(clip)
                                viewModel.generateKey("", KeyAlgorithm.ED25519, "") // dummy or snackbar
                            },
                            onDelete = { viewModel.deleteKey(keyRecord.id) },
                            onShowSnackbar = { msg ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("SSH Public Key", keyRecord.publicKey)
                                clipboard.setPrimaryClip(clip)
                            }
                        )
                    }
                }
            }
        }
    }

    if (state.isGenerateDialogOpen) {
        GenerateKeyDialog(
            onDismiss = { viewModel.dismissGenerateDialog() },
            onGenerate = { label, algo, comment ->
                viewModel.generateKey(label, algo, comment)
            }
        )
    }

    if (state.isImportDialogOpen) {
        ImportKeyDialog(
            onDismiss = { viewModel.dismissImportDialog() },
            onImport = { label, privPem, pubOpenSsh, algo ->
                viewModel.importKey(label, privPem, pubOpenSsh, algo)
            }
        )
    }
}

@Composable
fun KeyCard(
    keyRecord: KeyRecord,
    onCopyPublicKey: () -> Unit,
    onDelete: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SecondaryAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = SecondaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = keyRecord.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceVariantDark
                    ) {
                        Text(
                            text = keyRecord.algorithm.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("SSH Public Key", keyRecord.publicKey)
                        clipboard.setPrimaryClip(clip)
                        copied = true
                    }
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy Public Key",
                        tint = if (copied) SuccessGreen else PrimaryAccent
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Key",
                        tint = ErrorRed.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fingerprint
            Text(
                text = "Fingerprint: ${keyRecord.fingerprint}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                color = TextSecondary
            )

            if (copied) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Public key copied to clipboard! Paste into your server's ~/.ssh/authorized_keys",
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessGreen
                )
            }
        }
    }
}

@Composable
fun GenerateKeyDialog(
    onDismiss: () -> Unit,
    onGenerate: (label: String, algorithm: KeyAlgorithm, comment: String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedAlgorithm by remember { mutableStateOf(KeyAlgorithm.ED25519) }

    val algorithms = listOf(
        KeyAlgorithm.ED25519 to "Ed25519 (Recommended - Fast & Modern)",
        KeyAlgorithm.RSA_4096 to "RSA 4096-bit (Legacy & High Compatibility)",
        KeyAlgorithm.RSA_2048 to "RSA 2048-bit",
        KeyAlgorithm.ECDSA_256 to "ECDSA NIST P-256"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate SSH Keypair") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Key Label (e.g. Work Laptop)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (e.g. user@phone)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Algorithm",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                algorithms.forEach { (algo, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAlgorithm == algo,
                            onClick = { selectedAlgorithm = algo }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGenerate(label.ifBlank { "optimus-${selectedAlgorithm.name.lowercase()}" }, selectedAlgorithm, comment)
                },
                enabled = label.isNotBlank()
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportKeyDialog(
    onDismiss: () -> Unit,
    onImport: (label: String, privateKeyPem: String, publicKeyOpenSsh: String, algorithm: KeyAlgorithm) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var privateKeyPem by remember { mutableStateOf("") }
    var publicKeyOpenSsh by remember { mutableStateOf("") }
    var selectedAlgorithm by remember { mutableStateOf(KeyAlgorithm.ED25519) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Existing Private Key") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. AWS Production Key)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = privateKeyPem,
                    onValueChange = { privateKeyPem = it },
                    label = { Text("Private Key (PEM / OpenSSH format)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                OutlinedTextField(
                    value = publicKeyOpenSsh,
                    onValueChange = { publicKeyOpenSsh = it },
                    label = { Text("Public Key (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onImport(label, privateKeyPem.trim(), publicKeyOpenSsh.trim(), selectedAlgorithm)
                },
                enabled = label.isNotBlank() && privateKeyPem.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
