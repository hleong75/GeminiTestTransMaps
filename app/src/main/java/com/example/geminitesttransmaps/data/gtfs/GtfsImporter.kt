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
import java.io.FilterInputStream
import java.io.InputStreamReader
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
            val headers = parser.headerMap.keys
            val batch = ArrayList<StopEntity>(BATCH_SIZE)
            for (record in parser) {
                val stopId = record.getOptional(headers, "stop_id") ?: continue
                val stopName = record.getOptional(headers, "stop_name") ?: continue
                val stopLat = record.getOptional(headers, "stop_lat")?.toDoubleOrNull() ?: continue
                val stopLon = record.getOptional(headers, "stop_lon")?.toDoubleOrNull() ?: continue
                batch.add(
                    StopEntity(
                        stopId = stopId,
                        stopCode = record.getOptional(headers, "stop_code"),
                        stopName = stopName,
                        stopDesc = record.getOptional(headers, "stop_desc"),
                        stopLat = stopLat,
                        stopLon = stopLon,
                        zoneId = record.getOptional(headers, "zone_id"),
                        stopUrl = record.getOptional(headers, "stop_url"),
                        locationType = record.getOptional(headers, "location_type")?.toIntOrNull(),
                        parentStation = record.getOptional(headers, "parent_station"),
                        wheelchairBoarding = record.getOptional(headers, "wheelchair_boarding")?.toIntOrNull(),
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
            val headers = parser.headerMap.keys
            val batch = ArrayList<RouteEntity>(BATCH_SIZE)
            for (record in parser) {
                val routeId = record.getOptional(headers, "route_id") ?: continue
                val routeType = record.getOptional(headers, "route_type")?.toIntOrNull() ?: continue
                batch.add(
                    RouteEntity(
                        routeId = routeId,
                        agencyId = record.getOptional(headers, "agency_id"),
                        routeShortName = record.getOptional(headers, "route_short_name"),
                        routeLongName = record.getOptional(headers, "route_long_name"),
                        routeDesc = record.getOptional(headers, "route_desc"),
                        routeType = routeType,
                        routeUrl = record.getOptional(headers, "route_url"),
                        routeColor = record.getOptional(headers, "route_color"),
                        routeTextColor = record.getOptional(headers, "route_text_color"),
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
            val headers = parser.headerMap.keys
            val batch = ArrayList<TripEntity>(BATCH_SIZE)
            for (record in parser) {
                val routeId = record.getOptional(headers, "route_id") ?: continue
                val serviceId = record.getOptional(headers, "service_id") ?: continue
                val tripId = record.getOptional(headers, "trip_id") ?: continue
                batch.add(
                    TripEntity(
                        tripId = tripId,
                        routeId = routeId,
                        serviceId = serviceId,
                        tripHeadsign = record.getOptional(headers, "trip_headsign"),
                        tripShortName = record.getOptional(headers, "trip_short_name"),
                        directionId = record.getOptional(headers, "direction_id")?.toIntOrNull(),
                        blockId = record.getOptional(headers, "block_id"),
                        shapeId = record.getOptional(headers, "shape_id"),
                        wheelchairAccessible = record.getOptional(headers, "wheelchair_accessible")?.toIntOrNull(),
                        bikesAllowed = record.getOptional(headers, "bikes_allowed")?.toIntOrNull(),
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
            val headers = parser.headerMap.keys
            val batch = ArrayList<StopTimeEntity>(BATCH_SIZE)
            for (record in parser) {
                val tripId = record.getOptional(headers, "trip_id") ?: continue
                val stopId = record.getOptional(headers, "stop_id") ?: continue
                val stopSequence = record.getOptional(headers, "stop_sequence")?.toIntOrNull() ?: continue
                val arrivalTimeSec = parseTimeToSeconds(record.getOptional(headers, "arrival_time")) ?: continue
                val departureTimeSec = parseTimeToSeconds(record.getOptional(headers, "departure_time")) ?: continue
                batch.add(
                    StopTimeEntity(
                        tripId = tripId,
                        arrivalTimeSec = arrivalTimeSec,
                        departureTimeSec = departureTimeSec,
                        stopId = stopId,
                        stopSequence = stopSequence,
                        stopHeadsign = record.getOptional(headers, "stop_headsign"),
                        pickupType = record.getOptional(headers, "pickup_type")?.toIntOrNull(),
                        dropOffType = record.getOptional(headers, "drop_off_type")?.toIntOrNull(),
                        shapeDistTraveled = record.getOptional(headers, "shape_dist_traveled")?.toDoubleOrNull(),
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
            val headers = parser.headerMap.keys
            val batch = ArrayList<CalendarEntity>(BATCH_SIZE)
            for (record in parser) {
                val serviceId = record.getOptional(headers, "service_id") ?: continue
                val monday = record.getOptional(headers, "monday")?.toIntOrNull() ?: continue
                val tuesday = record.getOptional(headers, "tuesday")?.toIntOrNull() ?: continue
                val wednesday = record.getOptional(headers, "wednesday")?.toIntOrNull() ?: continue
                val thursday = record.getOptional(headers, "thursday")?.toIntOrNull() ?: continue
                val friday = record.getOptional(headers, "friday")?.toIntOrNull() ?: continue
                val saturday = record.getOptional(headers, "saturday")?.toIntOrNull() ?: continue
                val sunday = record.getOptional(headers, "sunday")?.toIntOrNull() ?: continue
                val startDate = record.getOptional(headers, "start_date") ?: continue
                val endDate = record.getOptional(headers, "end_date") ?: continue
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
        if (hours < 0) {
            return null
        }
        if (minutes !in 0..59 || seconds !in 0..59) {
            return null
        }
        return (hours * 3600) + (minutes * 60) + seconds
    }

    private fun csvParser(zipInputStream: ZipInputStream): CSVParser {
        return CSVParser(
            InputStreamReader(nonClosingInputStream(zipInputStream), Charsets.UTF_8),
            CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim(),
        )
    }

    private fun nonClosingInputStream(zipInputStream: ZipInputStream): FilterInputStream {
        return object : FilterInputStream(zipInputStream) {
            override fun close() {
                // No-op to keep ZipInputStream open for subsequent entries.
            }
        }
    }

    private fun CSVRecord.getOptional(headers: Set<String>, column: String): String? {
        if (!headers.contains(column) || !isSet(column)) {
            return null
        }
        return get(column).takeIf { it.isNotBlank() }
    }

    companion object {
        const val BATCH_SIZE = 500
    }
}
