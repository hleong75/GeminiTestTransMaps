package com.example.geminitesttransmaps.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GtfsDao {
    @Query(
        """
        SELECT * FROM stops
        WHERE stop_lat BETWEEN :minLat AND :maxLat
        AND stop_lon BETWEEN :minLon AND :maxLon
        """,
    )
    suspend fun getStopsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<StopEntity>

    @Query(
        """
        SELECT * FROM stop_times
        WHERE stop_id = :stopId
        AND arrival_time_sec >= :timeSec
        ORDER BY arrival_time_sec
        LIMIT :limit
        """,
    )
    suspend fun getStopTimesForStopAtTime(
        stopId: String,
        timeSec: Int,
        limit: Int = DEFAULT_STOP_TIMES_LIMIT,
    ): List<StopTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<StopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<RouteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopTimes(stopTimes: List<StopTimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendars(calendars: List<CalendarEntity>)

    companion object {
        const val DEFAULT_STOP_TIMES_LIMIT = 50
    }
}
