package com.example.geminitesttransmaps.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stops",
    indices = [
        Index(value = ["stop_lat", "stop_lon"]),
    ],
)
data class StopEntity(
    @PrimaryKey
    @ColumnInfo(name = "stop_id")
    val stopId: String,
    @ColumnInfo(name = "stop_code")
    val stopCode: String? = null,
    @ColumnInfo(name = "stop_name")
    val stopName: String,
    @ColumnInfo(name = "stop_desc")
    val stopDesc: String? = null,
    @ColumnInfo(name = "stop_lat")
    val stopLat: Double,
    @ColumnInfo(name = "stop_lon")
    val stopLon: Double,
    @ColumnInfo(name = "zone_id")
    val zoneId: String? = null,
    @ColumnInfo(name = "stop_url")
    val stopUrl: String? = null,
    @ColumnInfo(name = "location_type")
    val locationType: Int? = null,
    @ColumnInfo(name = "parent_station")
    val parentStation: String? = null,
    @ColumnInfo(name = "wheelchair_boarding")
    val wheelchairBoarding: Int? = null,
)

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey
    @ColumnInfo(name = "route_id")
    val routeId: String,
    @ColumnInfo(name = "agency_id")
    val agencyId: String? = null,
    @ColumnInfo(name = "route_short_name")
    val routeShortName: String? = null,
    @ColumnInfo(name = "route_long_name")
    val routeLongName: String? = null,
    @ColumnInfo(name = "route_desc")
    val routeDesc: String? = null,
    @ColumnInfo(name = "route_type")
    val routeType: Int,
    @ColumnInfo(name = "route_url")
    val routeUrl: String? = null,
    @ColumnInfo(name = "route_color")
    val routeColor: String? = null,
    @ColumnInfo(name = "route_text_color")
    val routeTextColor: String? = null,
)

@Entity(
    tableName = "trips",
    indices = [
        Index(value = ["route_id"]),
        Index(value = ["service_id"]),
    ],
)
data class TripEntity(
    @PrimaryKey
    @ColumnInfo(name = "trip_id")
    val tripId: String,
    @ColumnInfo(name = "route_id")
    val routeId: String,
    @ColumnInfo(name = "service_id")
    val serviceId: String,
    @ColumnInfo(name = "trip_headsign")
    val tripHeadsign: String? = null,
    @ColumnInfo(name = "trip_short_name")
    val tripShortName: String? = null,
    @ColumnInfo(name = "direction_id")
    val directionId: Int? = null,
    @ColumnInfo(name = "block_id")
    val blockId: String? = null,
    @ColumnInfo(name = "shape_id")
    val shapeId: String? = null,
    @ColumnInfo(name = "wheelchair_accessible")
    val wheelchairAccessible: Int? = null,
    @ColumnInfo(name = "bikes_allowed")
    val bikesAllowed: Int? = null,
)

@Entity(
    tableName = "stop_times",
    primaryKeys = ["trip_id", "stop_sequence"],
    indices = [
        Index(value = ["stop_id", "departure_time_sec"]),
    ],
)
data class StopTimeEntity(
    @ColumnInfo(name = "trip_id")
    val tripId: String,
    @ColumnInfo(name = "arrival_time_sec")
    val arrivalTimeSec: Int,
    @ColumnInfo(name = "departure_time_sec")
    val departureTimeSec: Int,
    @ColumnInfo(name = "stop_id")
    val stopId: String,
    @ColumnInfo(name = "stop_sequence")
    val stopSequence: Int,
    @ColumnInfo(name = "stop_headsign")
    val stopHeadsign: String? = null,
    @ColumnInfo(name = "pickup_type")
    val pickupType: Int? = null,
    @ColumnInfo(name = "drop_off_type")
    val dropOffType: Int? = null,
    @ColumnInfo(name = "shape_dist_traveled")
    val shapeDistTraveled: Double? = null,
)

@Entity(
    tableName = "calendar",
    indices = [
        Index(value = ["start_date"]),
        Index(value = ["end_date"]),
    ],
)
data class CalendarEntity(
    @PrimaryKey
    @ColumnInfo(name = "service_id")
    val serviceId: String,
    @ColumnInfo(name = "monday")
    val monday: Int,
    @ColumnInfo(name = "tuesday")
    val tuesday: Int,
    @ColumnInfo(name = "wednesday")
    val wednesday: Int,
    @ColumnInfo(name = "thursday")
    val thursday: Int,
    @ColumnInfo(name = "friday")
    val friday: Int,
    @ColumnInfo(name = "saturday")
    val saturday: Int,
    @ColumnInfo(name = "sunday")
    val sunday: Int,
    @ColumnInfo(name = "start_date")
    val startDate: String,
    @ColumnInfo(name = "end_date")
    val endDate: String,
)
