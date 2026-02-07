package com.example.geminitesttransmaps.domain.routing

import com.example.geminitesttransmaps.data.local.GtfsDao
import com.example.geminitesttransmaps.data.local.StopTimeEntity
import com.example.geminitesttransmaps.data.local.TripEntity

data class DirectTrip(
    val trip: TripEntity,
    val departureTimeSec: Int,
    val arrivalTimeSec: Int,
)

class RoutingEngine(
    private val dao: GtfsDao,
    private val maxStopTimes: Int = DEFAULT_MAX_STOP_TIMES,
) {
    suspend fun findDirectTrips(
        startStopId: String,
        endStopId: String,
        departureTimeSec: Int,
    ): List<DirectTrip> {
        if (startStopId == endStopId) {
            return emptyList()
        }
        val startStopTimes = dao.getStopTimesForStopAtTime(
            stopId = startStopId,
            timeSec = departureTimeSec,
            limit = maxStopTimes,
        )
        if (startStopTimes.isEmpty()) {
            return emptyList()
        }
        val tripIds = startStopTimes.map { it.tripId }.toSet().toList()
        val endStopTimes = loadStopTimesForTrips(endStopId, tripIds)
        if (endStopTimes.isEmpty()) {
            return emptyList()
        }
        val tripsById = loadTripsByIds(tripIds).associateBy { it.tripId }
        val startTimesByTrip = startStopTimes.groupBy { it.tripId }
        val endTimesByTrip = endStopTimes.groupBy { it.tripId }
        return tripIds.mapNotNull { tripId ->
            val trip = tripsById[tripId] ?: return@mapNotNull null
            val startTime = startTimesByTrip[tripId]
                ?.filter { it.departureTimeSec >= departureTimeSec }
                ?.minWithOrNull(
                    compareBy<StopTimeEntity> { it.departureTimeSec }
                        .thenBy { it.stopSequence },
                )
                ?: return@mapNotNull null
            val endTime = endTimesByTrip[tripId]
                ?.filter {
                    it.stopSequence > startTime.stopSequence &&
                        it.arrivalTimeSec >= startTime.departureTimeSec
                }
                ?.minByOrNull { it.arrivalTimeSec }
                ?: return@mapNotNull null
            DirectTrip(
                trip = trip,
                departureTimeSec = startTime.departureTimeSec,
                arrivalTimeSec = endTime.arrivalTimeSec,
            )
        }.sortedBy { it.arrivalTimeSec }
    }

    companion object {
        const val DEFAULT_MAX_STOP_TIMES = 1000
        private const val MAX_TRIP_ID_CHUNK = 900
    }

    private suspend fun loadStopTimesForTrips(
        stopId: String,
        tripIds: List<String>,
    ): List<StopTimeEntity> {
        return tripIds.chunked(MAX_TRIP_ID_CHUNK).flatMap { chunk ->
            dao.getStopTimesForTripsAtStop(stopId, chunk)
        }
    }

    private suspend fun loadTripsByIds(tripIds: List<String>): List<TripEntity> {
        return tripIds.chunked(MAX_TRIP_ID_CHUNK).flatMap { chunk ->
            dao.getTripsByIds(chunk)
        }
    }
}
