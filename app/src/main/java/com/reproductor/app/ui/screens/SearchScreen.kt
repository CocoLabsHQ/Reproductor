package com.reproductor.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reproductor.app.ui.MainViewModel
import com.reproductor.app.ui.components.EmptyState
import com.reproductor.app.ui.components.TrackRow

@Composable
fun SearchScreen(vm: MainViewModel) {
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val state by vm.playerState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Text(
            "Buscar", style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { vm.searchQuery.value = it },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            placeholder = { Text("Canción, artista, álbum, género…") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.searchQuery.value = "" }) { Icon(Icons.Rounded.Close, "Borrar") }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        when {
            query.isBlank() -> EmptyState("Escribe para buscar en tu biblioteca.")
            results.isEmpty() -> EmptyState("Sin resultados para «$query».")
            else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(results, key = { it.id }) { t ->
                    TrackRow(t, isCurrent = state.currentId == t.id, onClick = { vm.play(results, results.indexOf(t)) })
                }
            }
        }
    }
}
