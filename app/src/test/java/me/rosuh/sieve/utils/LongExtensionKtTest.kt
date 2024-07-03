package me.rosuh.sieve.utils

import kotlinx.datetime.*
import org.junit.Assert.*

import org.junit.Test
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class LongExtensionKtTest {

    private val currentMoment
        get() = Clock.System.now()
    
    private val currentTimestamp
        get() = currentMoment.toEpochMilliseconds()

    @Test
    fun calculateDuration() {

        // 测试刚刚
        assertEquals(currentTimestamp.calculateDuration(currentMoment), "刚刚")

        // 测试30秒前
        val thirtySecondsAgo =
            currentTimestamp - 30L.toDuration(DurationUnit.SECONDS).inWholeMilliseconds
        assertEquals(thirtySecondsAgo.calculateDuration(currentMoment), "30秒前")

        // 测试1分钟前
        val oneMinuteAgo =
            currentTimestamp - 1L.toDuration(DurationUnit.MINUTES).inWholeMilliseconds
        assertEquals(oneMinuteAgo.calculateDuration(currentMoment), "1分钟前")

        // 测试59分钟前
        val fiftyNineMinutesAgo =
            currentTimestamp - 59L.toDuration(DurationUnit.MINUTES).inWholeMilliseconds
        assertEquals(fiftyNineMinutesAgo.calculateDuration(currentMoment), "59分钟前")

        // 测试1小时前
        val oneHourAgo = currentTimestamp - 1L.toDuration(DurationUnit.HOURS).inWholeMilliseconds
        assertEquals(oneHourAgo.calculateDuration(currentMoment), "1小时前")

        // 测试23小时前
        val twentyThreeHoursAgo =
            currentTimestamp - 23L.toDuration(DurationUnit.HOURS).inWholeMilliseconds
        assertEquals(twentyThreeHoursAgo.calculateDuration(currentMoment), "23小时前")

        // 测试1天前
        val oneDayAgo = currentTimestamp - 1L.toDuration(DurationUnit.DAYS).inWholeMilliseconds
        assertEquals(oneDayAgo.calculateDuration(currentMoment), "1天前")

        // 测试6天前
        val sixDaysAgo = currentTimestamp - 6L.toDuration(DurationUnit.DAYS).inWholeMilliseconds
        assertEquals(sixDaysAgo.calculateDuration(currentMoment), "6天前")

        // 测试1周前
        val oneWeekAgo = currentTimestamp - 7L.toDuration(DurationUnit.DAYS).inWholeMilliseconds
        assertEquals(oneWeekAgo.calculateDuration(currentMoment), "7天前")

        // 测试2周前
        val twoWeeksAgo = currentTimestamp - 14L.toDuration(DurationUnit.DAYS).inWholeMilliseconds
        assertEquals(twoWeeksAgo.calculateDuration(currentMoment), "14天前")

        // 测试1个月前
        val oneMonthAgo = currentTimestamp - 30L.toDuration(DurationUnit.DAYS).inWholeMilliseconds
        assertEquals(oneMonthAgo.calculateDuration(currentMoment), "30天前")

        // 测试2个月前
        val twoMonthsAgo = currentTimestamp - 60L.toDuration(DurationUnit.DAYS).inWholeMilliseconds
        assertEquals(twoMonthsAgo.calculateDuration(currentMoment), "60天前")

        // 测试1年前
        val oneYearAgo = currentTimestamp - 365L.toDuration(DurationUnit.DAYS).inWholeMilliseconds
        assertEquals(oneYearAgo.calculateDuration(currentMoment), "365天前")

        // 测试2年前
        val twoYearsAgo =
            currentTimestamp - (2 * 365L).toDuration(DurationUnit.DAYS).inWholeMilliseconds
        assertEquals(twoYearsAgo.calculateDuration(currentMoment), "730天前")

        val mishap = 1L.toDuration(DurationUnit.SECONDS).inWholeMilliseconds
        // 测试未来30秒
        val thirtySecondsLater =
            currentTimestamp + 30L.toDuration(DurationUnit.SECONDS).inWholeMilliseconds + mishap
        assertEquals(thirtySecondsLater.calculateDuration(currentMoment), "30秒后")

        // 测试未来1分钟
        val oneMinuteLater =
            currentTimestamp + 1L.toDuration(DurationUnit.MINUTES).inWholeMilliseconds + mishap
        assertEquals(oneMinuteLater.calculateDuration(currentMoment), "1分钟后")

        // 测试未来1小时
        val oneHourLater = currentTimestamp + 1L.toDuration(DurationUnit.HOURS).inWholeMilliseconds + mishap
        assertEquals(oneHourLater.calculateDuration(currentMoment), "1小时后")

        // 测试未来1天
        val oneDayLater = currentTimestamp + 1L.toDuration(DurationUnit.DAYS).inWholeMilliseconds + mishap
        assertEquals(oneDayLater.calculateDuration(currentMoment), "1天后")

        // 测试未来1周
        val oneWeekLater = currentTimestamp + 7L.toDuration(DurationUnit.DAYS).inWholeMilliseconds + mishap
        assertEquals(oneWeekLater.calculateDuration(currentMoment), "7天后")

        // 测试未来1个月
        val oneMonthLater = currentTimestamp + 30L.toDuration(DurationUnit.DAYS).inWholeMilliseconds + mishap
        assertEquals(oneMonthLater.calculateDuration(currentMoment), "30天后")

        // 测试未来1年
        val oneYearLater = currentTimestamp + 365L.toDuration(DurationUnit.DAYS).inWholeMilliseconds + mishap
        assertEquals(oneYearLater.calculateDuration(currentMoment), "365天后")
    }
}