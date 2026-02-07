package com.example.geminitesttransmaps.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        StopEntity::class,
        RouteEntity::class,
        TripEntity::class,
        StopTimeEntity::class,
        CalendarEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gtfsDao(): GtfsDao

    companion object {
        const val DATABASE_NAME = "transmaps.db"
    }
}
