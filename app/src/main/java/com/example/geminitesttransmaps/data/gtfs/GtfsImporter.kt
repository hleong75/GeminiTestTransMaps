package com.example.geminitesttransmaps.data.gtfs

import android.content.Context
import android.net.Uri
import com.example.geminitesttransmaps.data.local.CalendarEntity
import com.example.geminitesttransmaps.data.local.GtfsDao
import com.example.geminitesttransmaps.data.local.RouteEntity
import com.example.geminitesttransmaps.data.local.StopEntity
import com.example.geminitesttransmaps.data.local.StopTimeEntity
import com.example.geminitesttransmaps.data.local.TripEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.BufferedInputStream
import java.io.InputStreamReader
import java.io.Reader
import java.util.zip.ZipInputStream

class GtfsImporter(
    private val context: Context,
    private val dao: GtfsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun import(uri: Uri): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipInputStream ->
                    parseZipEntries(zipInputStream)
                }
            } ?: error("Unable to open GTFS zip from uri: $uri")
        }
    }

    private suspend fun parseZipEntries(zipInputStream: ZipInputStream) {
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                when (entry.name.substringAfterLast('/').lowercase()) {
                    "stops.txt" -> parseStops(zipInputStream)
                    "routes.txt" -> parseRoutes(zipInputStream)
                    "trips.txt" -> parseTrips(zipInputStream)
                    "stop_times.txt" -> parseStopTimes(zipInputStream)
                    "calendar.txt" -> parseCalendar(zipInputStream)
                }
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
    }

    private suspend fun parseStops(zipInputStream: ZipInputStream) {
        csvParser(zipInputStream).use { parser ->
            val batch = ArrayList<StopEntity>(BATCH_SIZE)
            for (record in parser) {
                val stopId = record.getOptional("stop_id") ?: continue
                val stopName = record.getOptional("stop_name") ?: continue
                val stopLat = record.getOptional("stop_lat")?.toDoubleOrNull() ?: continue
                val stopLon = record.getOptional("stop_lon")?.toDoubleOrNull() ?: continue
                batch.add(
                    StopEntity(
                        stopId = stopId,
                        stopCode = record.getOptional("stop_code"),
                        stopName = stopName,
                        stopDesc = record.getOptional("stop_desc"),
                        stopLat = stopLat,
                        stopLon = stopLon,
                        zoneId = record.getOptional("zone_id"),
                        stopUrl = record.getOptional("stop_url"),
                        locationType = record.getOptional("location_type")?.toIntOrNull(),
                        parentStation = record.getOptional("parent_station"),
                        wheelchairBoarding = record.getOptional("wheelchair_boarding")?.toIntOrNull(),
                    ),
                )
                if (batch.size >= BATCH_SIZE) {
                    dao.insertStops(batch)
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) {
                dao.insertStops(batch)
            }
        }
    }

    private suspend fun parseRoutes(zipInputStream: ZipInputStream) {
        csvParser(zipInputStream).use { parser ->
            val batch = ArrayList<RouteEntity>(BATCH_SIZE)
            for (record in parser) {
                val routeId = record.getOptional("route_id") ?: continue
                val routeType = record.getOptional("route_type")?.toIntOrNull() ?: continue
                batch.add(
                    RouteEntity(
                        routeId = routeId,
                        agencyId = record.getOptional("agency_id"),
                        routeShortName = record.getOptional("route_short_name"),
                        routeLongName = record.getOptional("route_long_name"),
                        routeDesc = record.getOptional("route_desc"),
                        routeType = routeType,
                        routeUrl = record.getOptional("route_url"),
                        routeColor = record.getOptional("route_color"),
                        routeTextColor = record.getOptional("route_text_color"),
                    ),
                )
                if (batch.size >= BATCH_SIZE) {
                    dao.insertRoutes(batch)
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) {
                dao.insertRoutes(batch)
            }
        }
    }

    private suspend fun parseTrips(zipInputStream: ZipInputStream) {
        csvParser(zipInputStream).use { parser ->
            val batch = ArrayList<TripEntity>(BATCH_SIZE)
            for (record in parser) {
                val routeId = record.getOptional("route_id") ?: continue
                val serviceId = record.getOptional("service_id") ?: continue
                val tripId = record.getOptional("trip_id") ?: continue
                batch.add(
                    TripEntity(
                        tripId = tripId,
                        routeId = routeId,
                        serviceId = serviceId,
                        tripHeadsign = record.getOptional("trip_headsign"),
                        tripShortName = record.getOptional("trip_short_name"),
                        directionId = record.getOptional("direction_id")?.toIntOrNull(),
                        blockId = record.getOptional("block_id"),
                        shapeId = record.getOptional("shape_id"),
                        wheelchairAccessible = record.getOptional("wheelchair_accessible")?.toIntOrNull(),
                        bikesAllowed = record.getOptional("bikes_allowed")?.toIntOrNull(),
                    ),
                )
                if (batch.size >= BATCH_SIZE) {
                    dao.insertTrips(batch)
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) {
                dao.insertTrips(batch)
            }
        }
    }

    private suspend fun parseStopTimes(zipInputStream: ZipInputStream) {
        csvParser(zipInputStream).use { parser ->
            val batch = ArrayList<StopTimeEntity>(BATCH_SIZE)
            for (record in parser) {
                val tripId = record.getOptional("trip_id") ?: continue
                val stopId = record.getOptional("stop_id") ?: continue
                val stopSequence = record.getOptional("stop_sequence")?.toIntOrNull() ?: continue
                val arrivalTimeSec = parseTimeToSeconds(record.getOptional("arrival_time")) ?: continue
                val departureTimeSec = parseTimeToSeconds(record.getOptional("departure_time")) ?: continue
                batch.add(
                    StopTimeEntity(
                        tripId = tripId,
                        arrivalTimeSec = arrivalTimeSec,
                        departureTimeSec = departureTimeSec,
                        stopId = stopId,
                        stopSequence = stopSequence,
                        stopHeadsign = record.getOptional("stop_headsign"),
                        pickupType = record.getOptional("pickup_type")?.toIntOrNull(),
                        dropOffType = record.getOptional("drop_off_type")?.toIntOrNull(),
                        shapeDistTraveled = record.getOptional("shape_dist_traveled")?.toDoubleOrNull(),
                    ),
                )
                if (batch.size >= BATCH_SIZE) {
                    dao.insertStopTimes(batch)
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) {
                dao.insertStopTimes(batch)
            }
        }
    }

    private suspend fun parseCalendar(zipInputStream: ZipInputStream) {
        csvParser(zipInputStream).use { parser ->
            val batch = ArrayList<CalendarEntity>(BATCH_SIZE)
            for (record in parser) {
                val serviceId = record.getOptional("service_id") ?: continue
                val monday = record.getOptional("monday")?.toIntOrNull() ?: continue
                val tuesday = record.getOptional("tuesday")?.toIntOrNull() ?: continue
                val wednesday = record.getOptional("wednesday")?.toIntOrNull() ?: continue
                val thursday = record.getOptional("thursday")?.toIntOrNull() ?: continue
                val friday = record.getOptional("friday")?.toIntOrNull() ?: continue
                val saturday = record.getOptional("saturday")?.toIntOrNull() ?: continue
                val sunday = record.getOptional("sunday")?.toIntOrNull() ?: continue
                val startDate = record.getOptional("start_date") ?: continue
                val endDate = record.getOptional("end_date") ?: continue
                batch.add(
                    CalendarEntity(
                        serviceId = serviceId,
                        monday = monday,
                        tuesday = tuesday,
                        wednesday = wednesday,
                        thursday = thursday,
                        friday = friday,
                        saturday = saturday,
                        sunday = sunday,
                        startDate = startDate,
                        endDate = endDate,
                    ),
                )
                if (batch.size >= BATCH_SIZE) {
                    dao.insertCalendars(batch)
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) {
                dao.insertCalendars(batch)
            }
        }
    }

    private fun parseTimeToSeconds(timeValue: String?): Int? {
        val parts = timeValue?.split(":") ?: return null
        if (parts.size != 3) {
            return null
        }
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        val seconds = parts[2].toIntOrNull() ?: return null
        if (hours < 0 || minutes !in 0..59 || seconds !in 0..59) {
            return null
        }
        return (hours * 3600) + (minutes * 60) + seconds
    }

    private fun csvParser(zipInputStream: ZipInputStream): CSVParser {
        return CSVParser(
            nonClosingReader(zipInputStream),
            CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim(),
        )
    }

    private fun nonClosingReader(zipInputStream: ZipInputStream): Reader {
        return object : InputStreamReader(zipInputStream, Charsets.UTF_8) {
            override fun close() {
                // No-op to keep ZipInputStream open for subsequent entries.
            }
        }
    }

    private fun CSVRecord.getOptional(column: String): String? {
        if (!isMapped(column) || !isSet(column)) {
            return null
        }
        return get(column).takeIf { it.isNotBlank() }
    }

    companion object {
        const val BATCH_SIZE = 500
    }
}
