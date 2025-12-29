package io.github.mrsfantaus.godot.healthconnect

import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.*
import org.godotengine.godot.Dictionary
import java.time.Instant
import java.time.ZoneOffset
import java.time.Duration

class DataTypeMapper {

    fun mapToRecord(dict: Dictionary): Record? {
        val type = dict["type"] as? String ?: return null
        val startTimeStr = dict["start_time"] as? String
        val endTimeStr = dict["end_time"] as? String
        val timeStr = dict["time"] as? String
        val id = dict["id"] as? String
        
        val startInstant = startTimeStr?.let { Instant.parse(it) } ?: Instant.now()
        val endInstant = endTimeStr?.let { Instant.parse(it) } ?: Instant.now()
        val instant = timeStr?.let { Instant.parse(it) } ?: Instant.now()

        val metadata = if (id != null) {
            Metadata(id)
        } else {
            Metadata()
        }

        return when (type) {
            "STEPS" -> StepsRecord(
                count = (dict["count"] as? Number)?.toLong() ?: 0,
                startTime = startInstant,
                endTime = endInstant,
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata = metadata
            )
            "DISTANCE" -> DistanceRecord(
                distance = Length.meters((dict["value"] as? Number)?.toDouble() ?: 0.0),
                startTime = startInstant,
                endTime = endInstant,
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata = metadata
            )
            "ACTIVE_CALORIES_BURNED" -> ActiveCaloriesBurnedRecord(
                energy = Energy.kilocalories((dict["value"] as? Number)?.toDouble() ?: 0.0),
                startTime = startInstant,
                endTime = endInstant,
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata = metadata
            )
            "WEIGHT" -> WeightRecord(
                weight = Mass.kilograms((dict["value"] as? Number)?.toDouble() ?: 0.0),
                time = instant,
                zoneOffset = ZoneOffset.UTC,
                metadata = metadata
            )
            "HEART_RATE" -> {
                val samples = dict["samples"] as? Array<*>
                val heartRateSamples = samples?.mapNotNull {
                    val s = it as? Dictionary ?: return@mapNotNull null
                    HeartRateRecord.Sample(
                        time = Instant.parse(s["time"] as String),
                        beatsPerMinute = (s["value"] as Number).toLong()
                    )
                } ?: emptyList()
                HeartRateRecord(
                    startTime = startInstant,
                    endTime = endInstant,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    samples = heartRateSamples,
                    metadata = metadata
                )
            }
            "EXERCISE_SESSION" -> ExerciseSessionRecord(
                startTime = startInstant,
                endTime = endInstant,
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                exerciseType = (dict["exercise_type"] as? Number)?.toInt() ?: ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
                title = dict["title"] as? String,
                notes = dict["notes"] as? String,
                metadata = metadata
            )
            "SLEEP_SESSION" -> SleepSessionRecord(
                startTime = startInstant,
                endTime = endInstant,
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                title = dict["title"] as? String,
                notes = dict["notes"] as? String,
                metadata = metadata
            )
            else -> null
        }
    }

    fun mapFromRecord(record: Record): Dictionary {
        val dict = Dictionary()
        dict["id"] = record.metadata.id
        dict["data_origin"] = record.metadata.dataOrigin.packageName

        when (record) {
            is StepsRecord -> {
                dict["type"] = "STEPS"
                dict["count"] = record.count
                dict["start_time"] = record.startTime.toString()
                dict["end_time"] = record.endTime.toString()
            }
            is DistanceRecord -> {
                dict["type"] = "DISTANCE"
                dict["value"] = record.distance.inMeters
                dict["start_time"] = record.startTime.toString()
                dict["end_time"] = record.endTime.toString()
            }
            is ActiveCaloriesBurnedRecord -> {
                dict["type"] = "ACTIVE_CALORIES_BURNED"
                dict["value"] = record.energy.inKilocalories
                dict["start_time"] = record.startTime.toString()
                dict["end_time"] = record.endTime.toString()
            }
            is WeightRecord -> {
                dict["type"] = "WEIGHT"
                dict["value"] = record.weight.inKilograms
                dict["time"] = record.time.toString()
            }
            is HeartRateRecord -> {
                dict["type"] = "HEART_RATE"
                dict["start_time"] = record.startTime.toString()
                dict["end_time"] = record.endTime.toString()
                val samples = record.samples.map {
                    val s = Dictionary()
                    s["time"] = it.time.toString()
                    s["value"] = it.beatsPerMinute
                    s
                }
                dict["samples"] = samples.toTypedArray()
            }
            is ExerciseSessionRecord -> {
                dict["type"] = "EXERCISE_SESSION"
                dict["start_time"] = record.startTime.toString()
                dict["end_time"] = record.endTime.toString()
                dict["exercise_type"] = record.exerciseType
                dict["title"] = record.title
                dict["notes"] = record.notes
            }
            is SleepSessionRecord -> {
                dict["type"] = "SLEEP_SESSION"
                dict["start_time"] = record.startTime.toString()
                dict["end_time"] = record.endTime.toString()
                dict["title"] = record.title
                dict["notes"] = record.notes
            }
        }
        return dict
    }
}
