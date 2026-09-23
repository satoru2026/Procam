package com.procam.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.procam.app.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            SettingRow("Simpan lokasi GPS pada foto", state.saveLocation) { viewModel.toggleSaveLocation() }
            SettingRow("Watermark tanggal & nama app", state.watermarkEnabled) { viewModel.toggleWatermark() }
            SettingRow("Suara rana", state.shutterSoundEnabled) { viewModel.toggleShutterSound() }
            SettingRow("HDR otomatis", state.hdrEnabled) { viewModel.toggleHdr() }

            Text(
                "ProCam v1.0 — kamera Android open-source dengan kontrol manual, " +
                    "mode malam, potret, panorama, slow-motion, dan filter warna.",
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
