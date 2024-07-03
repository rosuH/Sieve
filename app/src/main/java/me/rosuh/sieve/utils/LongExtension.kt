package me.rosuh.sieve.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant


fun Long.calculateDuration(currentMoment: Instant = Clock.System.now()): String {
    val thisMoment = Instant.fromEpochMilliseconds(this)
    val duration = (currentMoment - thisMoment)
    val absDuration = duration.absoluteValue
    val suffix = if (duration.isPositive()) "前" else "后"
    return when {
        absDuration.inWholeDays > 0 -> {
            "${absDuration.inWholeDays}天$suffix"
        }

        absDuration.inWholeHours > 0 -> {
            "${absDuration.inWholeHours}小时$suffix"
        }

        absDuration.inWholeMinutes > 0 -> {
            "${absDuration.inWholeMinutes}分钟$suffix"
        }

        absDuration.inWholeSeconds >= 30 -> {
            "${absDuration.inWholeSeconds}秒$suffix"
        }

        else -> {
            "刚刚"
        }
    }
}