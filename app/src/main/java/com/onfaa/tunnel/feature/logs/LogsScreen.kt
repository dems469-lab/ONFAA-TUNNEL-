package com.onfaa.tunnel.feature.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.vpn.TunnelStatusRepository

@Composable
fun LogsScreen() {
    val events by TunnelStatusRepository.events.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Journaux", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { TunnelStatusRepository.clearEvents() }) {
                Text("Effacer")
            }
        }
        Text(
            text = "Événements réels du cycle de vie du tunnel. Aucun identifiant, UUID ou lien n'est journalisé.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (events.isEmpty()) {
            Text(
                text = "Aucun événement pour cette session.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(events.asReversed()) { event ->
                    Text(
                        text = "${event.timeLabel()}  ${event.message}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
