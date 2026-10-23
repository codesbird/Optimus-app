package com.optimus.ssh.ui.hosts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optimus.ssh.model.AuthType
import com.optimus.ssh.model.Host
import com.optimus.ssh.model.HostStatus
import com.optimus.ssh.ui.theme.*

data class HostPlatformInfo(
    val name: String,
    val icon: ImageVector,
    val tint: Color
)

fun detectHostPlatform(host: Host): HostPlatformInfo {
    val text = "${host.label} ${host.hostname} ${host.tags.joinToString(" ")}".lowercase()
    return when {
        text.contains("aws") || text.contains("ec2") || text.contains("amazon") ->
            HostPlatformInfo("AWS EC2", Icons.Default.Cloud, Color(0xFFFF9900))
        text.contains("contabo") ->
            HostPlatformInfo("Contabo", Icons.Default.Storage, Color(0xFF00B0FF))
        text.contains("digitalocean") || text.contains("droplet") || text.contains("ocean") ->
            HostPlatformInfo("DigitalOcean", Icons.Default.CloudQueue, Color(0xFF0080FF))
        text.contains("local") || text.contains("home") || host.hostname == "127.0.0.1" || host.hostname.startsWith("192.168.") || host.hostname.startsWith("10.") ->
            HostPlatformInfo("Local / LAN", Icons.Default.Home, Color(0xFF4CAF50))
        text.contains("windows") || text.contains("wsl") || text.contains("win") ->
            HostPlatformInfo("Windows", Icons.Default.Computer, Color(0xFF00A4EF))
        text.contains("ubuntu") || text.contains("debian") || text.contains("linux") || text.contains("centos") ->
            HostPlatformInfo("Linux", Icons.Default.Terminal, PrimaryAccent)
        else ->
            HostPlatformInfo("Server", Icons.Default.Dns, SecondaryAccent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostsScreen(
    viewModel: HostsViewModel,
    onNavigateToTerminal: (String) -> Unit,
    onLockApp: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Optimus",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = PrimaryAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SSH",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLockApp) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock app",
                            tint = TextSecondary
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
                onClick = { viewModel.openAddHostDialog() },
                containerColor = PrimaryAccent,
                contentColor = BackgroundDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Host")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search hosts, IPs, or tags…", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = SurfaceVariantDark,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            } else if (state.filteredHosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.searchQuery.isEmpty()) "No SSH hosts configured" else "No matching hosts found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to add your first server",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredHosts, key = { it.id }) { host ->
                        val jumpHost = state.hosts.find { it.id == host.jumpHostId }
                        HostCard(
                            host = host,
                            jumpHostName = jumpHost?.label,
                            onConnect = {
                                onNavigateToTerminal(host.id)
                            },
                            onEdit = { viewModel.openEditHostDialog(host) },
                            onDelete = { viewModel.deleteHost(host.id) }
                        )
                    }
                }
            }
        }
    }

    if (state.isAddEditDialogOpen) {
        AddEditHostDialog(
            hostToEdit = state.hostToEdit,
            availableHosts = state.hosts,
            availableIdentities = state.availableIdentities,
            availableKeys = state.availableKeys,
            onDismiss = { viewModel.dismissAddEditDialog() },
            onSave = { label, hostname, port, username, password, authType, identityId, privateKeyId, jumpHostId, tags ->
                viewModel.saveHost(
                    label = label,
                    hostname = hostname,
                    port = port,
                    username = username,
                    password = password,
                    authType = authType,
                    identityId = identityId,
                    privateKeyId = privateKeyId,
                    jumpHostId = jumpHostId,
                    tags = tags
                )
            }
        )
    }
}

@Composable
fun HostCard(
    host: Host,
    jumpHostName: String? = null,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val platform = detectHostPlatform(host)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Platform Icon with Connection Status Overlay Dot
            val (statusColor, showStatusDot) = when (host.status) {
                HostStatus.AUTHENTICATED, HostStatus.CONNECTED -> SuccessGreen to true
                HostStatus.CONNECTING -> WarningYellow to true
                HostStatus.ERROR -> ErrorRed to true
                HostStatus.DISCONNECTED -> Color.Transparent to false
            }

            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                // Platform Icon Container
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(platform.tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = platform.icon,
                        contentDescription = platform.name,
                        tint = platform.tint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Status Indicator Dot (Bottom-Right)
                if (showStatusDot) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BackgroundDark)
                            .padding(1.5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${host.username}@${host.hostname}:${host.port}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = PrimaryAccent
                )

                // Badges row: Jump Host, Identity, Key, Tags
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (jumpHostName != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryAccent.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.AltRoute,
                                    contentDescription = null,
                                    tint = PrimaryAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "via $jumpHostName",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = PrimaryAccent
                                )
                            }
                        }
                    }

                    if (host.identityId != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SecondaryAccent.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = SecondaryAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Identity",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = SecondaryAccent
                                )
                            }
                        }
                    } else if (host.authType == AuthType.KEY) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SecondaryAccent.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = SecondaryAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Key",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = SecondaryAccent
                                )
                            }
                        }
                    }

                    host.tags.take(2).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceVariantDark
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Menu (Edit, Delete)
            Box {
                IconButton(
                    onClick = { expandedMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    modifier = Modifier.background(SurfaceVariantDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Host", color = TextPrimary) },
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
    }
}
