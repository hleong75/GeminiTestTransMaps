package com.example.geminitesttransmaps.data.gtfs

import android.net.Uri
import com.example.geminitesttransmaps.data.local.GtfsDao
import com.example.geminitesttransmaps.data.local.StopEntity
import com.example.geminitesttransmaps.data.local.StopTimeEntity

class GtfsRepository(
    private val dao: GtfsDao,
    private val importer: GtfsImporter,
) {
    suspend fun importGtfs(uri: Uri): Result<Unit> = importer.import(uri)

    suspend fun getStopsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<StopEntity> {
        return dao.getStopsInBoundingBox(
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
        )
    }

    suspend fun getStopTimesForStopAtTime(
        stopId: String,
        timeSec: Int,
        limit: Int = GtfsDao.DEFAULT_STOP_TIMES_LIMIT,
    ): List<StopTimeEntity> {
        return dao.getStopTimesForStopAtTime(
            stopId = stopId,
            timeSec = timeSec,
            limit = limit,
        )
    }
}
