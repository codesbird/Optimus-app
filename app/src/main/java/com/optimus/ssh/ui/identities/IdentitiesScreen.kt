package com.optimus.ssh.ui.identities

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optimus.ssh.model.AuthType
import com.optimus.ssh.model.Identity
import com.optimus.ssh.model.KeyRecord
import com.optimus.ssh.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentitiesScreen(
    viewModel: IdentitiesViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = PrimaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Identities",
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
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = PrimaryAccent,
                contentColor = BackgroundDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Identity")
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
            } else if (state.identities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No identities configured",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Save reusable credentials to quickly connect to your servers without retyping passwords or picking keys repeatedly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.openAddDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = BackgroundDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create First Identity", color = BackgroundDark)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.identities, key = { it.id }) { identity ->
                        val attachedKey = state.availableKeys.find { it.id == identity.privateKeyId }
                        IdentityCard(
                            identity = identity,
                            attachedKey = attachedKey,
                            onEdit = { viewModel.openEditDialog(identity) },
                            onDelete = { viewModel.deleteIdentity(identity.id) },
                            onCopyUsername = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Username", identity.username))
                            }
                        )
                    }
                }
            }
        }
    }

    if (state.isAddEditDialogOpen) {
        AddEditIdentityDialog(
            identityToEdit = state.identityToEdit,
            availableKeys = state.availableKeys,
            onDismiss = { viewModel.dismissAddEditDialog() },
            onSave = { label, username, authType, password, privateKeyId ->
                viewModel.saveIdentity(label, username, authType, password, privateKeyId)
            }
        )
    }
}

@Composable
fun IdentityCard(
    identity: Identity,
    attachedKey: KeyRecord?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyUsername: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PrimaryAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = identity.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = identity.username,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = PrimaryAccent
                        )
                        IconButton(
                            onClick = onCopyUsername,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy username",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        modifier = Modifier.background(SurfaceVariantDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextPrimary) },
                            onClick = {
                                expandedMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = ErrorRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                            onClick = {
                                expandedMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auth type chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceVariantDark
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (identity.authType) {
                            AuthType.PASSWORD -> Icons.Default.Lock
                            AuthType.KEY -> Icons.Default.VpnKey
                            AuthType.PASSWORD_AND_KEY -> Icons.Default.Security
                            AuthType.AGENT -> Icons.Default.Tune
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = SecondaryAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (identity.authType) {
                                AuthType.PASSWORD -> "Password"
                                AuthType.KEY -> "SSH Key"
                                AuthType.PASSWORD_AND_KEY -> "Password & Key"
                                AuthType.AGENT -> "Agent"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextPrimary
                        )
                    }
                }

                if (attachedKey != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariantDark
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = PrimaryAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${attachedKey.label} (${attachedKey.algorithm.name})",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIdentityDialog(
    identityToEdit: Identity?,
    availableKeys: List<KeyRecord>,
    onDismiss: () -> Unit,
    onSave: (label: String, username: String, authType: AuthType, password: String?, privateKeyId: String?) -> Unit
) {
    var label by remember { mutableStateOf(identityToEdit?.label ?: "") }
    var username by remember { mutableStateOf(identityToEdit?.username ?: "ubuntu") }
    var authType by remember { mutableStateOf(identityToEdit?.authType ?: AuthType.PASSWORD) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedKeyId by remember { mutableStateOf(identityToEdit?.privateKeyId) }
    var keyDropdownExpanded by remember { mutableStateOf(false) }

    val isFormValid = label.isNotBlank() && username.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp),
        title = {
            Text(
                text = if (identityToEdit == null) "New Identity" else "Edit Identity",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
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
                    label = { Text("Identity Label (e.g. AWS Staging)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Authentication Method",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryAccent
                )

                com.optimus.ssh.ui.hosts.SegmentedSelector(
                    options = listOf(
                        AuthType.PASSWORD to "Password",
                        AuthType.KEY to "SSH Key",
                        AuthType.PASSWORD_AND_KEY to "Both"
                    ),
                    selected = authType,
                    onSelect = { authType = it }
                )

                // Password field if required
                if (authType == AuthType.PASSWORD || authType == AuthType.PASSWORD_AND_KEY) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                if (identityToEdit == null) "Password"
                                else "New Password (leave empty to keep)"
                            )
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Key dropdown if required
                if (authType == AuthType.KEY || authType == AuthType.PASSWORD_AND_KEY) {
                    val currentKey = availableKeys.find { it.id == selectedKeyId }

                    ExposedDropdownMenuBox(
                        expanded = keyDropdownExpanded,
                        onExpandedChange = { keyDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentKey?.let { "${it.label} (${it.algorithm.name})" } ?: "Select an SSH Key…",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("SSH Key") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = keyDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = keyDropdownExpanded,
                            onDismissRequest = { keyDropdownExpanded = false }
                        ) {
                            if (availableKeys.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No keys available (add one in Keys tab)") },
                                    onClick = { keyDropdownExpanded = false },
                                    enabled = false
                                )
                            } else {
                                availableKeys.forEach { key ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(key.label, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "${key.algorithm.name} • ${key.fingerprint.take(20)}…",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedKeyId = key.id
                                            keyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        label,
                        username,
                        authType,
                        password.ifEmpty { null },
                        selectedKeyId
                    )
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = BackgroundDark)
            ) {
                Text(if (identityToEdit == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
