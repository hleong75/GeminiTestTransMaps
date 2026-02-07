package com.example.geminitesttransmaps.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminitesttransmaps.data.gtfs.GtfsRepository
import com.example.geminitesttransmaps.data.local.StopEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val stops: List<StopEntity> = emptyList(),
    val stopsById: Map<String, StopEntity> = emptyMap(),
    val searchQuery: String = "",
    val searchResults: List<StopEntity> = emptyList(),
    val isSearchActive: Boolean = false,
    val selectedStop: StopEntity? = null,
    val routeSummary: String? = null,
    val gtfsImportStatus: ImportStatus? = null,
    val mbtilesUri: String? = null,
    val styleUri: String = DEFAULT_STYLE_URI,
)

enum class ImportStatus {
    Success,
    Failure,
}

class MainViewModel(
    private val repository: GtfsRepository? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val results = if (query.isBlank()) {
                emptyList()
            } else {
                state.stops.filter { stop ->
                    stop.stopName.contains(query, ignoreCase = true) ||
                        stop.stopId.contains(query, ignoreCase = true)
                }
            }
            state.copy(searchQuery = query, searchResults = results)
        }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active) }
    }

    fun onSearchSubmit() {
        _uiState.update { it.copy(isSearchActive = false) }
    }

    fun selectStop(stop: StopEntity) {
        _uiState.update { it.copy(selectedStop = stop, isSearchActive = false) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedStop = null, routeSummary = null) }
    }

    fun setStops(stops: List<StopEntity>) {
        _uiState.update { state ->
            state.copy(
                stops = stops,
                stopsById = stops.associateBy { entity -> entity.stopId },
            )
        }
    }

    fun setMbtilesUri(uri: Uri) {
        _uiState.update { it.copy(mbtilesUri = uri.toString()) }
    }

    fun importGtfs(uri: Uri) {
        viewModelScope.launch {
            if (repository == null) {
                _uiState.update { it.copy(gtfsImportStatus = ImportStatus.Failure) }
                return@launch
            }
            val result = repository.importGtfs(uri)
            _uiState.update { state ->
                state.copy(
                    gtfsImportStatus = result.fold(
                        onSuccess = { ImportStatus.Success },
                        onFailure = { ImportStatus.Failure },
                    ),
                )
            }
        }
    }

    companion object {
        const val DEFAULT_STYLE_URI = "asset://style.json"
    }
}
