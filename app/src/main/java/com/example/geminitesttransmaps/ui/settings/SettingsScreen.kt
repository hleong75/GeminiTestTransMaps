package com.example.geminitesttransmaps.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.geminitesttransmaps.R
import com.example.geminitesttransmaps.ui.ImportStatus
import com.example.geminitesttransmaps.ui.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onImportGtfs: (Uri) -> Unit,
    onImportMbtiles: (Uri) -> Unit,
    onBack: () -> Unit,
) {
    val gtfsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let(onImportGtfs)
    }
    val mbtilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let(onImportMbtiles)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    Text(
                        text = stringResource(id = R.string.back_button_label),
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .clickable { onBack() },
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Button(onClick = { gtfsLauncher.launch(GTFS_MIME_TYPE) }) {
                Text(text = stringResource(id = R.string.import_gtfs_button))
            }
            uiState.gtfsImportStatus?.let { status ->
                val statusText = when (status) {
                    ImportStatus.Success -> stringResource(id = R.string.import_status_success)
                    ImportStatus.Failure -> stringResource(id = R.string.import_status_failure)
                }
                Text(text = statusText, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { mbtilesLauncher.launch(MBTILES_MIME_TYPE) }) {
                Text(text = stringResource(id = R.string.import_mbtiles_button))
            }
            uiState.mbtilesUri?.let { uri ->
                Text(
                    text = stringResource(id = R.string.selected_mbtiles_label, uri),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private const val GTFS_MIME_TYPE = "application/zip"
private const val MBTILES_MIME_TYPE = "*/*"
