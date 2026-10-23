package com.optimus.ssh

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.optimus.ssh.ui.hosts.HostsViewModel
import com.optimus.ssh.ui.identities.IdentitiesViewModel
import com.optimus.ssh.ui.keys.KeysViewModel
import com.optimus.ssh.ui.navigation.OptimusNavGraph
import com.optimus.ssh.ui.theme.OptimusTheme

class MainActivity : FragmentActivity() {

    private val app by lazy { OptimusApplication.instance }

    private val hostsViewModel: HostsViewModel by viewModels {
        HostsViewModel.Factory(
            hostRepository = app.hostRepository,
            identityRepository = app.identityRepository,
            keyRepository = app.keyRepository,
            sshConnectionManager = app.sshConnectionManager
        )
    }

    private val keysViewModel: KeysViewModel by viewModels {
        KeysViewModel.Factory(
            keyRepository = app.keyRepository
        )
    }

    private val identitiesViewModel: IdentitiesViewModel by viewModels {
        IdentitiesViewModel.Factory(
            identityRepository = app.identityRepository,
            keyRepository = app.keyRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OptimusTheme {
                val navController = rememberNavController()
                OptimusNavGraph(
                    navController = navController,
                    activity = this,
                    hostsViewModel = hostsViewModel,
                    keysViewModel = keysViewModel,
                    identitiesViewModel = identitiesViewModel
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            app.sshConnectionManager.disconnectAll()
        }
    }
}
