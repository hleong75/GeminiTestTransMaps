package com.example.geminitesttransmaps.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.geminitesttransmaps.R
import com.example.geminitesttransmaps.data.local.StopEntity
import com.example.geminitesttransmaps.ui.MainUiState
import com.example.geminitesttransmaps.ui.map.MapScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchSubmit: () -> Unit,
    onStopSelected: (StopEntity) -> Unit,
    onClearSelection: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (uiState.selectedStop != null || uiState.routeSummary != null) {
        ModalBottomSheet(
            onDismissRequest = onClearSelection,
            sheetState = sheetState,
        ) {
            val selectedStop = uiState.selectedStop
            if (selectedStop != null) {
                ListItem(
                    headlineContent = { Text(selectedStop.stopName) },
                    supportingContent = { Text(selectedStop.stopId) },
                )
                selectedStop.stopDesc?.let { desc ->
                    Text(
                        text = desc,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                Text(
                    text = uiState.routeSummary.orEmpty(),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapScreen(
            styleUri = uiState.styleUri,
            stops = uiState.stops,
            stopsById = uiState.stopsById,
            onStopSelected = onStopSelected,
            modifier = Modifier.fillMaxSize(),
        )
        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = { onSearchSubmit() },
            active = uiState.isSearchActive,
            onActiveChange = onSearchActiveChange,
            placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
            leadingIcon = { Text(stringResource(id = R.string.search_icon_placeholder)) },
            trailingIcon = {
                Text(
                    text = stringResource(id = R.string.settings_icon_placeholder),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable { onOpenSettings() },
                )
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = SearchBarDefaults.colors(),
        ) {
            if (uiState.searchQuery.isNotBlank() && uiState.searchResults.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.search_no_results),
                    modifier = Modifier.padding(16.dp),
                )
            }
            LazyColumn {
                items(uiState.searchResults) { stop ->
                    ListItem(
                        headlineContent = { Text(stop.stopName) },
                        supportingContent = { Text(stop.stopId) },
                        modifier = Modifier.clickable { onStopSelected(stop) },
                    )
                }
            }
        }
        if (!uiState.isSearchActive) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = SEARCH_BAR_OVERLAY_PADDING),
                tonalElevation = 4.dp,
                shape = SearchBarDefaults.inputFieldShape,
            ) {
                Text(
                    text = stringResource(id = R.string.offline_map_label),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private val SEARCH_BAR_OVERLAY_PADDING = 84.dp
