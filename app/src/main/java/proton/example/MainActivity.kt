/*
 * Copyright (c) 2024 Proton AG
 *
 * This file is part of ProtonVPN.
 *
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */

package proton.example

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.ini4j.InvalidFileFormatException
import proton.example.ui.theme.ProtonVPNExampleTheme
import proton.example.vpn.VpnState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProtonVPNExampleTheme {
                val vpnState = viewModel.state.collectAsStateWithLifecycle(lifecycle).value
                MainScreen(
                    onConnect = ::prepareAndConnect,
                    onDisconnect = ::disconnect,
                    state = vpnState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }
    }

    private fun prepareAndConnect(config: String) {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            connect(config)
        } else {
            val launcher = activityResultRegistry.register(
                "VPNPermission", VpnPermissionContract(intent)
            ) { permissionGranted ->
                if (permissionGranted)
                    connect(config)
            }
            try {
                launcher.launch(Unit)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "This device doesn't support VPN connections", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connect(config: String) {
        fun onError(e: Throwable) {
            Toast.makeText(this, "Invalid config: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        try {
            viewModel.connect(config)
        } catch (e: NoSuchElementException) {
            onError(e)
        } catch (e: InvalidFileFormatException) {
            onError(e)
        }
    }

    private fun disconnect() {
        viewModel.disconnect()
    }
}

class VpnPermissionContract(private val intent: Intent) : ActivityResultContract<Unit, Boolean>() {
    override fun createIntent(context: Context, input: Unit): Intent = intent
    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}

@Composable
fun MainScreen(
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    state: VpnState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val configText = rememberSaveable { mutableStateOf("") }
        TextField(
            value = configText.value,
            onValueChange = { configText.value = it },
            label = { Text("WireGuard config") },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "VPN State: $state")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    when (state) {
                        VpnState.DOWN -> onConnect(configText.value)
                        VpnState.UP -> onDisconnect()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = configText.value.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (state) {
                        VpnState.DOWN -> MaterialTheme.colorScheme.primary
                        VpnState.UP -> MaterialTheme.colorScheme.error
                    }
                )
            ) {
                when (state) {
                    VpnState.DOWN -> Text("Connect")
                    VpnState.UP -> Text("Disconnect")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ProtonVPNExampleTheme {
        MainScreen({}, {}, VpnState.UP)
    }
}