package com.optimus.ssh.ui.hosts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.optimus.ssh.model.AuthType
import com.optimus.ssh.model.Host
import com.optimus.ssh.model.Identity
import com.optimus.ssh.model.KeyRecord
import com.optimus.ssh.ui.theme.*

private enum class CredentialMode {
    DIRECT,
    IDENTITY
}

@Composable
fun <T> SegmentedSelector(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceVariantDark,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(3.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryAccent else Color.Transparent)
                        .clickable { onSelect(value) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) BackgroundDark else TextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHostDialog(
    hostToEdit: Host?,
    availableHosts: List<Host>,
    availableIdentities: List<Identity>,
    availableKeys: List<KeyRecord>,
    onDismiss: () -> Unit,
    onSave: (
        label: String,
        hostname: String,
        port: Int,
        username: String,
        password: String?,
        authType: AuthType,
        identityId: String?,
        privateKeyId: String?,
        jumpHostId: String?,
        tags: List<String>
    ) -> Unit
) {
    var label by remember { mutableStateOf(hostToEdit?.label ?: "") }
    var hostname by remember { mutableStateOf(hostToEdit?.hostname ?: "") }
    var portText by remember { mutableStateOf(hostToEdit?.port?.toString() ?: "22") }
    var username by remember { mutableStateOf(hostToEdit?.username ?: "root") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var tagsText by remember { mutableStateOf(hostToEdit?.tags?.joinToString(", ") ?: "") }

    // Authentication mode: Direct vs Identity
    var credMode by remember {
        mutableStateOf(
            if (hostToEdit?.identityId != null) CredentialMode.IDENTITY else CredentialMode.DIRECT
        )
    }
    var selectedIdentityId by remember { mutableStateOf(hostToEdit?.identityId) }
    var identityDropdownExpanded by remember { mutableStateOf(false) }

    // Direct auth settings
    var directAuthType by remember { mutableStateOf(hostToEdit?.authType ?: AuthType.PASSWORD) }
    var selectedKeyId by remember { mutableStateOf(hostToEdit?.privateKeyId) }
    var keyDropdownExpanded by remember { mutableStateOf(false) }

    // Bastion / Jump Host settings (exclude this host from candidate bastions)
    val candidateJumpHosts = remember(availableHosts, hostToEdit) {
        availableHosts.filter { it.id != hostToEdit?.id }
    }
    var selectedJumpHostId by remember { mutableStateOf(hostToEdit?.jumpHostId) }
    var jumpHostDropdownExpanded by remember { mutableStateOf(false) }

    val isPortValid = portText.toIntOrNull() != null
    val isFormValid = label.isNotBlank() && hostname.isNotBlank() && isPortValid && when (credMode) {
        CredentialMode.IDENTITY -> selectedIdentityId != null
        CredentialMode.DIRECT -> username.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp),
        title = {
            Text(
                text = if (hostToEdit == null) "New SSH Host" else "Edit SSH Host",
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
                // Host Label
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. Production Web)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Hostname & Port
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hostname,
                        onValueChange = { hostname = it },
                        label = { Text("Hostname / IP") },
                        singleLine = true,
                        modifier = Modifier.weight(0.72f)
                    )
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.28f)
                    )
                }

                HorizontalDivider(color = SurfaceVariantDark)

                // Credential Mode Selector
                Text(
                    text = "Credentials",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryAccent
                )

                SegmentedSelector(
                    options = listOf(
                        CredentialMode.DIRECT to "Direct Credentials",
                        CredentialMode.IDENTITY to "Saved Identity"
                    ),
                    selected = credMode,
                    onSelect = { credMode = it }
                )

                if (credMode == CredentialMode.IDENTITY) {
                    val currentIdentity = availableIdentities.find { it.id == selectedIdentityId }

                    ExposedDropdownMenuBox(
                        expanded = identityDropdownExpanded,
                        onExpandedChange = { identityDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentIdentity?.let { "${it.label} (${it.username})" } ?: "Select an Identity…",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Identity Profile") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = identityDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = identityDropdownExpanded,
                            onDismissRequest = { identityDropdownExpanded = false }
                        ) {
                            if (availableIdentities.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No saved identities (create one in Identities tab)") },
                                    onClick = { identityDropdownExpanded = false },
                                    enabled = false
                                )
                            } else {
                                availableIdentities.forEach { idItem ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(idItem.label, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "User: ${idItem.username} • Auth: ${idItem.authType.name}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedIdentityId = idItem.id
                                            identityDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Direct Credentials
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SegmentedSelector(
                        options = listOf(
                            AuthType.PASSWORD to "Password",
                            AuthType.KEY to "SSH Key",
                            AuthType.PASSWORD_AND_KEY to "Both"
                        ),
                        selected = directAuthType,
                        onSelect = { directAuthType = it }
                    )

                    if (directAuthType == AuthType.PASSWORD || directAuthType == AuthType.PASSWORD_AND_KEY) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(if (hostToEdit == null) "Password" else "New Password (leave empty to keep)") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (directAuthType == AuthType.KEY || directAuthType == AuthType.PASSWORD_AND_KEY) {
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
                                        text = { Text("No SSH keys available (generate or import in Keys tab)") },
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

                HorizontalDivider(color = SurfaceVariantDark)

                // Jump Host / Bastion Chaining (ProxyJump)
                Text(
                    text = "ProxyJump / Bastion Host",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryAccent
                )

                val currentJumpHost = candidateJumpHosts.find { it.id == selectedJumpHostId }

                ExposedDropdownMenuBox(
                    expanded = jumpHostDropdownExpanded,
                    onExpandedChange = { jumpHostDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentJumpHost?.let { "${it.label} (${it.hostname})" } ?: "None (Direct connection)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Jump Host (Bastion)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jumpHostDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = jumpHostDropdownExpanded,
                        onDismissRequest = { jumpHostDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Direct connection)") },
                            onClick = {
                                selectedJumpHostId = null
                                jumpHostDropdownExpanded = false
                            }
                        )
                        candidateJumpHosts.forEach { bastion ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(bastion.label, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${bastion.username}@${bastion.hostname}:${bastion.port}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                },
                                onClick = {
                                    selectedJumpHostId = bastion.id
                                    jumpHostDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Tags
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Tags (comma separated, e.g. aws, prod)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: 22
                    val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    if (credMode == CredentialMode.IDENTITY) {
                        val identity = availableIdentities.find { it.id == selectedIdentityId }
                        val effectiveUsername = identity?.username ?: username
                        val effectiveAuthType = identity?.authType ?: AuthType.PASSWORD
                        val effectiveKeyId = identity?.privateKeyId

                        onSave(
                            label,
                            hostname,
                            port,
                            effectiveUsername,
                            null,
                            effectiveAuthType,
                            selectedIdentityId,
                            effectiveKeyId,
                            selectedJumpHostId,
                            tags
                        )
                    } else {
                        onSave(
                            label,
                            hostname,
                            port,
                            username,
                            password.ifEmpty { null },
                            directAuthType,
                            null,
                            selectedKeyId,
                            selectedJumpHostId,
                            tags
                        )
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = BackgroundDark)
            ) {
                Text(if (hostToEdit == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
