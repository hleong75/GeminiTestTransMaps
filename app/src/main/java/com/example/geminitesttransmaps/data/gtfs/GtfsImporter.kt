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
                val stopId = record.getNonBlankOrNull(headers, "stop_id") ?: continue
                val stopName = record.getNonBlankOrNull(headers, "stop_name") ?: continue
                val stopLat = record.getNonBlankOrNull(headers, "stop_lat")?.toDoubleOrNull() ?: continue
                val stopLon = record.getNonBlankOrNull(headers, "stop_lon")?.toDoubleOrNull() ?: continue
                batch.add(
                    StopEntity(
                        stopId = stopId,
                        stopCode = record.getNonBlankOrNull(headers, "stop_code"),
                        stopName = stopName,
                        stopDesc = record.getNonBlankOrNull(headers, "stop_desc"),
                        stopLat = stopLat,
                        stopLon = stopLon,
                        zoneId = record.getNonBlankOrNull(headers, "zone_id"),
                        stopUrl = record.getNonBlankOrNull(headers, "stop_url"),
                        locationType = record.getNonBlankOrNull(headers, "location_type")?.toIntOrNull(),
                        parentStation = record.getNonBlankOrNull(headers, "parent_station"),
                        wheelchairBoarding = record.getNonBlankOrNull(headers, "wheelchair_boarding")?.toIntOrNull(),
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
                val routeId = record.getNonBlankOrNull(headers, "route_id") ?: continue
                val routeType = record.getNonBlankOrNull(headers, "route_type")?.toIntOrNull() ?: continue
                batch.add(
                    RouteEntity(
                        routeId = routeId,
                        agencyId = record.getNonBlankOrNull(headers, "agency_id"),
                        routeShortName = record.getNonBlankOrNull(headers, "route_short_name"),
                        routeLongName = record.getNonBlankOrNull(headers, "route_long_name"),
                        routeDesc = record.getNonBlankOrNull(headers, "route_desc"),
                        routeType = routeType,
                        routeUrl = record.getNonBlankOrNull(headers, "route_url"),
                        routeColor = record.getNonBlankOrNull(headers, "route_color"),
                        routeTextColor = record.getNonBlankOrNull(headers, "route_text_color"),
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
                val routeId = record.getNonBlankOrNull(headers, "route_id") ?: continue
                val serviceId = record.getNonBlankOrNull(headers, "service_id") ?: continue
                val tripId = record.getNonBlankOrNull(headers, "trip_id") ?: continue
                batch.add(
                    TripEntity(
                        tripId = tripId,
                        routeId = routeId,
                        serviceId = serviceId,
                        tripHeadsign = record.getNonBlankOrNull(headers, "trip_headsign"),
                        tripShortName = record.getNonBlankOrNull(headers, "trip_short_name"),
                        directionId = record.getNonBlankOrNull(headers, "direction_id")?.toIntOrNull(),
                        blockId = record.getNonBlankOrNull(headers, "block_id"),
                        shapeId = record.getNonBlankOrNull(headers, "shape_id"),
                        wheelchairAccessible = record.getNonBlankOrNull(headers, "wheelchair_accessible")?.toIntOrNull(),
                        bikesAllowed = record.getNonBlankOrNull(headers, "bikes_allowed")?.toIntOrNull(),
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
                val tripId = record.getNonBlankOrNull(headers, "trip_id") ?: continue
                val stopId = record.getNonBlankOrNull(headers, "stop_id") ?: continue
                val stopSequence = record.getNonBlankOrNull(headers, "stop_sequence")?.toIntOrNull() ?: continue
                val arrivalTimeSec = parseTimeToSeconds(record.getNonBlankOrNull(headers, "arrival_time")) ?: continue
                val departureTimeSec = parseTimeToSeconds(record.getNonBlankOrNull(headers, "departure_time")) ?: continue
                batch.add(
                    StopTimeEntity(
                        tripId = tripId,
                        arrivalTimeSec = arrivalTimeSec,
                        departureTimeSec = departureTimeSec,
                        stopId = stopId,
                        stopSequence = stopSequence,
                        stopHeadsign = record.getNonBlankOrNull(headers, "stop_headsign"),
                        pickupType = record.getNonBlankOrNull(headers, "pickup_type")?.toIntOrNull(),
                        dropOffType = record.getNonBlankOrNull(headers, "drop_off_type")?.toIntOrNull(),
                        shapeDistTraveled = record.getNonBlankOrNull(headers, "shape_dist_traveled")?.toDoubleOrNull(),
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
                val serviceId = record.getNonBlankOrNull(headers, "service_id") ?: continue
                val monday = record.getNonBlankOrNull(headers, "monday")?.toIntOrNull() ?: continue
                val tuesday = record.getNonBlankOrNull(headers, "tuesday")?.toIntOrNull() ?: continue
                val wednesday = record.getNonBlankOrNull(headers, "wednesday")?.toIntOrNull() ?: continue
                val thursday = record.getNonBlankOrNull(headers, "thursday")?.toIntOrNull() ?: continue
                val friday = record.getNonBlankOrNull(headers, "friday")?.toIntOrNull() ?: continue
                val saturday = record.getNonBlankOrNull(headers, "saturday")?.toIntOrNull() ?: continue
                val sunday = record.getNonBlankOrNull(headers, "sunday")?.toIntOrNull() ?: continue
                val startDate = record.getNonBlankOrNull(headers, "start_date") ?: continue
                val endDate = record.getNonBlankOrNull(headers, "end_date") ?: continue
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
        if (hours > MAX_GTFS_HOURS) {
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

    private fun CSVRecord.getNonBlankOrNull(headers: Set<String>, column: String): String? {
        if (!headers.contains(column) || !isSet(column)) {
            return null
        }
        return get(column).takeIf { it.isNotBlank() }
    }

    companion object {
        const val BATCH_SIZE = 500
        const val MAX_GTFS_HOURS = 48
    }
}
