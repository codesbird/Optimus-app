package com.optimus.ssh.ui.navigation

import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.optimus.ssh.OptimusApplication
import com.optimus.ssh.ui.hosts.HostsViewModel
import com.optimus.ssh.ui.identities.IdentitiesViewModel
import com.optimus.ssh.ui.keys.KeysViewModel
import com.optimus.ssh.ui.lock.AppLockScreen
import com.optimus.ssh.ui.main.MainScreen

object Routes {
    const val LOCK = "lock"
    const val MAIN = "main"
    const val TERMINAL = "terminal/{hostId}"
    fun terminal(hostId: String) = "terminal/$hostId"
}

@Composable
fun OptimusNavGraph(
    navController: NavHostController,
    activity: FragmentActivity,
    hostsViewModel: HostsViewModel,
    keysViewModel: KeysViewModel,
    identitiesViewModel: IdentitiesViewModel
) {
    val app = OptimusApplication.instance

    NavHost(
        navController = navController,
        startDestination = Routes.LOCK
    ) {
        composable(Routes.LOCK) {
            AppLockScreen(
                onUnlockWithBiometrics = {
                    if (app.biometricHelper.isBiometricAvailable()) {
                        app.biometricHelper.promptBiometric(
                            activity = activity,
                            onSuccess = {
                                navController.navigate(Routes.MAIN) {
                                    popUpTo(Routes.LOCK) { inclusive = true }
                                }
                            },
                            onError = { /* Keep on lock screen */ }
                        )
                    } else {
                        // Fallback if no biometrics enrolled
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.LOCK) { inclusive = true }
                        }
                    }
                },
                onUnlocked = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOCK) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                hostsViewModel = hostsViewModel,
                keysViewModel = keysViewModel,
                identitiesViewModel = identitiesViewModel,
                onNavigateToTerminal = { hostId ->
                    navController.navigate(Routes.terminal(hostId))
                },
                onLockApp = {
                    navController.navigate(Routes.LOCK) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.TERMINAL) { backStackEntry ->
            val hostId = backStackEntry.arguments?.getString("hostId") ?: ""
            val terminalViewModel: com.optimus.ssh.terminal.TerminalViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                key = hostId,
                factory = com.optimus.ssh.terminal.TerminalViewModel.Factory(
                    hostId = hostId,
                    hostRepository = app.hostRepository,
                    keyRepository = app.keyRepository,
                    identityRepository = app.identityRepository,
                    sshConnectionManager = app.sshConnectionManager,
                    context = activity
                )
            )
            com.optimus.ssh.terminal.TerminalScreen(
                viewModel = terminalViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
