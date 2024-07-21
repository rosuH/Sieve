package me.rosuh.sieve.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.rosuh.sieve.R


@Composable
fun Long.calculateDurationComposable(): String {
    val currentMoment = Clock.System.now()
    val suffixBefore = stringResource(id = R.string.tab_home_scan_time_unit_before)
    val suffixAfter = stringResource(id = R.string.tab_home_scan_time_unit_after)
    val d = stringResource(id = R.string.tab_home_scan_time_unit_d)
    val h = stringResource(id = R.string.tab_home_scan_time_unit_h)
    val m = stringResource(id = R.string.tab_home_scan_time_unit_m)
    val s = stringResource(id = R.string.tab_home_scan_time_unit_s)
    val justNow = stringResource(id = R.string.tab_home_scan_time_unit_just_now)
    return calculateDuration(currentMoment, suffixBefore, suffixAfter, d, h, m, s, justNow)
}

fun Long.calculateDuration(
    currentMoment: Instant = Clock.System.now(),
    suffixBefore: String = "前",
    suffixAfter: String = "后",
    d: String = "天",
    h: String = "小时",
    m: String = "分钟",
    s: String = "秒",
    justNow: String = "刚刚"
): String {
    val thisMoment = Instant.fromEpochMilliseconds(this)
    val duration = (currentMoment - thisMoment)
    val absDuration = duration.absoluteValue
    val suffix =
        if (duration.isPositive()) {
            suffixBefore
        } else {
            suffixAfter
        }
    return when {
        absDuration.inWholeDays > 0 -> {
            "${absDuration.inWholeDays}${d}$suffix"
        }

        absDuration.inWholeHours > 0 -> {
            "${absDuration.inWholeHours}${h}$suffix"
        }

        absDuration.inWholeMinutes > 0 -> {
            "${absDuration.inWholeMinutes}${m}$suffix"
        }

        absDuration.inWholeSeconds >= 30 -> {
            "${absDuration.inWholeSeconds}${s}$suffix"
        }

        else -> {
            justNow
        }
    }
}