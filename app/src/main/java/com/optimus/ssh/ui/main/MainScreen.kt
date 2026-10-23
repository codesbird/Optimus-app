package com.optimus.ssh.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.optimus.ssh.ui.hosts.HostsScreen
import com.optimus.ssh.ui.hosts.HostsViewModel
import com.optimus.ssh.ui.identities.IdentitiesScreen
import com.optimus.ssh.ui.identities.IdentitiesViewModel
import com.optimus.ssh.ui.keys.KeysScreen
import com.optimus.ssh.ui.keys.KeysViewModel
import com.optimus.ssh.ui.theme.BackgroundDark
import com.optimus.ssh.ui.theme.PrimaryAccent
import com.optimus.ssh.ui.theme.SurfaceDark
import com.optimus.ssh.ui.theme.TextPrimary
import com.optimus.ssh.ui.theme.TextSecondary

enum class MainNavigationTab {
    HOSTS,
    KEYS,
    IDENTITIES
}

@Composable
fun MainScreen(
    hostsViewModel: HostsViewModel,
    keysViewModel: KeysViewModel,
    identitiesViewModel: IdentitiesViewModel,
    onNavigateToTerminal: (String) -> Unit,
    onLockApp: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainNavigationTab.HOSTS) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextPrimary
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainNavigationTab.HOSTS,
                    onClick = { selectedTab = MainNavigationTab.HOSTS },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Hosts"
                        )
                    },
                    label = {
                        Text(
                            text = "Hosts",
                            fontWeight = if (selectedTab == MainNavigationTab.HOSTS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BackgroundDark,
                        selectedTextColor = PrimaryAccent,
                        indicatorColor = PrimaryAccent,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == MainNavigationTab.KEYS,
                    onClick = { selectedTab = MainNavigationTab.KEYS },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = "Keys"
                        )
                    },
                    label = {
                        Text(
                            text = "Keys",
                            fontWeight = if (selectedTab == MainNavigationTab.KEYS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BackgroundDark,
                        selectedTextColor = PrimaryAccent,
                        indicatorColor = PrimaryAccent,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == MainNavigationTab.IDENTITIES,
                    onClick = { selectedTab = MainNavigationTab.IDENTITIES },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "Identities"
                        )
                    },
                    label = {
                        Text(
                            text = "Identities",
                            fontWeight = if (selectedTab == MainNavigationTab.IDENTITIES) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BackgroundDark,
                        selectedTextColor = PrimaryAccent,
                        indicatorColor = PrimaryAccent,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainNavigationTab.HOSTS -> HostsScreen(
                    viewModel = hostsViewModel,
                    onNavigateToTerminal = onNavigateToTerminal,
                    onLockApp = onLockApp
                )
                MainNavigationTab.KEYS -> KeysScreen(
                    viewModel = keysViewModel
                )
                MainNavigationTab.IDENTITIES -> IdentitiesScreen(
                    viewModel = identitiesViewModel
                )
            }
        }
    }
}
