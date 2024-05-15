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

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.ini4j.Ini
import proton.example.vpn.WireGuard
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val wireGuard: WireGuard
) : ViewModel() {
    val state = wireGuard.state

    fun connect(
        wgConfig: String
    ) {
        val parsedConfig = Ini(wgConfig.byteInputStream()).toMap()
        val iface = parsedConfig.getValue("Interface")
        val peer = parsedConfig.getValue("Peer")
        wireGuard.connect(
            peer = peer.entries.map { "${it.key} = ${it.value}" },
            iface = iface.entries.map { "${it.key} = ${it.value}" },
        )
    }

    fun disconnect() {
        wireGuard.disconnect()
    }
}