package com.procam.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.procam.app.camera.FilterType
import com.procam.app.camera.ManualSettings

@Composable
fun ManualControlsPanel(
    manual: ManualSettings,
    onToggleManual: () -> Unit,
    onIsoChange: (Int) -> Unit,
    onShutterChange: (Long) -> Unit,
    onFocusChange: (Float) -> Unit,
    onWbChange: (Int) -> Unit,
    onEvChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Mode Manual (Pro)", color = Color.White, modifier = Modifier.padding(end = 8.dp))
            Switch(checked = manual.isManualEnabled, onCheckedChange = { onToggleManual() })
        }

        if (manual.isManualEnabled) {
            LabeledSlider("ISO", manual.iso.toFloat(), 50f, 3200f) { onIsoChange(it.toInt()) }
            // shutter speed shown as fraction of a second, from 1/4000s to 1s
            val shutterSeconds = manual.shutterSpeedNs / 1_000_000_000f
            LabeledSlider(
                "Shutter (1/${(1f / shutterSeconds).toInt().coerceAtLeast(1)}s)",
                shutterSeconds, 1f / 4000f, 1f
            ) { onShutterChange((it * 1_000_000_000L).toLong()) }
            LabeledSlider("Fokus (0=infinity)", manual.focusDistanceDiopters, 0f, 10f) { onFocusChange(it) }
            LabeledSlider("White Balance (K)", manual.whiteBalanceKelvin.toFloat(), 2000f, 9000f) { onWbChange(it.toInt()) }
        }

        LabeledSlider("EV", manual.evCompensation.toFloat(), -6f, 6f) { onEvChange(it.toInt()) }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label: ${"%.2f".format(value)}", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}

@Composable
fun FilterStrip(selected: FilterType, onSelect: (FilterType) -> Unit) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        items(FilterType.entries) { f ->
            Text(
                text = f.label,
                color = if (f == selected) Color.Yellow else Color.White,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .clickable { onSelect(f) }
            )
        }
    }
}
