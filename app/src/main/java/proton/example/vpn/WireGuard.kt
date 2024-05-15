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

package proton.example.vpn

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

enum class VpnState {
    UP,
    DOWN,
}

@Singleton
class WireGuard @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val wgBackend = GoBackend(appContext)
    private val tunnel = WireGuardTunnel()

    private val wgScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val _state = MutableStateFlow(VpnState.DOWN)

    val state : StateFlow<VpnState> = _state

    fun connect(
        peer: List<String>,
        iface: List<String>,
    ) {
        val config = Config.Builder()
            .parsePeer(peer)
            .parseInterface(iface)
            .build()

        wgScope.launch {
            apiCall("https://example.com")
            wgBackend.setState(tunnel, Tunnel.State.UP, config)
            _state.value = VpnState.UP
            launch {
                delay(1000)
                apiCall("https://example.com")
            }
        }
    }

    fun disconnect() {
        wgScope.launch {
            wgBackend.setState(tunnel, Tunnel.State.DOWN, null)
            _state.value = VpnState.DOWN
        }
    }

    private fun apiCall(url: String) {
        val client = OkHttpClient()

        try {
            val request = Request.Builder()
                .url(url)
                .build();

            val response = client.newCall(request).execute()
            println("API response: $response")
        } catch (e: IOException) {
            println("API response error: ${e.message}")
        }
    }
}

private class WireGuardTunnel : Tunnel {
    override fun getName() = "VpnTunnel"
    override fun onStateChange(newState: Tunnel.State) {}
}