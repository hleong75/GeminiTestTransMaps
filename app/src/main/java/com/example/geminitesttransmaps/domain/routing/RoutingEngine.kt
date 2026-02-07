package com.example.geminitesttransmaps.domain.routing

import com.example.geminitesttransmaps.data.local.GtfsDao
import com.example.geminitesttransmaps.data.local.TripEntity

data class DirectTrip(
    val trip: TripEntity,
    val departureTimeSec: Int,
    val arrivalTimeSec: Int,
)

class RoutingEngine(
    private val dao: GtfsDao,
    private val maxConnections: Int = DEFAULT_MAX_CONNECTIONS,
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
            limit = maxConnections,
        )
        if (startStopTimes.isEmpty()) {
            return emptyList()
        }
        val tripIds = startStopTimes.map { it.tripId }.distinct()
        val endStopTimes = dao.getStopTimesForTripsAtStop(
            stopId = endStopId,
            tripIds = tripIds,
        )
        if (endStopTimes.isEmpty()) {
            return emptyList()
        }
        val tripsById = dao.getTripsByIds(tripIds).associateBy { it.tripId }
        val startTimesByTrip = startStopTimes
            .groupBy { it.tripId }
            .mapValues { (_, times) -> times.minByOrNull { it.departureTimeSec } }
        val endTimesByTrip = endStopTimes
            .groupBy { it.tripId }
            .mapValues { (_, times) -> times.minByOrNull { it.arrivalTimeSec } }
        return tripIds.mapNotNull { tripId ->
            val trip = tripsById[tripId] ?: return@mapNotNull null
            val startTime = startTimesByTrip[tripId] ?: return@mapNotNull null
            val endTime = endTimesByTrip[tripId] ?: return@mapNotNull null
            if (endTime.arrivalTimeSec <= startTime.departureTimeSec) {
                return@mapNotNull null
            }
            DirectTrip(
                trip = trip,
                departureTimeSec = startTime.departureTimeSec,
                arrivalTimeSec = endTime.arrivalTimeSec,
            )
        }.sortedBy { it.arrivalTimeSec }
    }

    companion object {
        const val DEFAULT_MAX_CONNECTIONS = 1000
    }
}
