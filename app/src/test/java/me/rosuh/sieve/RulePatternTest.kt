package me.rosuh.sieve

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.rosuh.sieve.model.RulePattern
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RulePatternTest {

    lateinit var packageList: List<String>

    private val ruleList by lazy { """
        REGEX,com.alibaba.*
        REGEX,com.tencent.*
        REGEX,com.taobao.*
        REGEX,com.qiyi.*
        REGEX,com.miui.*
        REGEX,tv.danmaku.*
        REGEX,com.xiaomi.*
        REGEX,com.miui.*
        REGEX,cn.gov.*
        REGEX,com.jingdong.*
        REGEX,com.jd.*
        EXTRA,com.tongcheng.android
        REGEX,com.sina
        REGEX,com.xueqiu.*
        REGEX,com.cmbchina.*
        REGEX,com.huawei.*
        EXTRA,com.flomo.app
        EXTRA,com.gotokeep.keep
        EXTRA,com.tplink.ipc
    """.trimIndent() }

    private lateinit var rulePatternList: List<RulePattern>

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun setUp() {
        val json = this.javaClass.classLoader?.getResource("output.json")
        packageList = Json.decodeFromStream<ArrayList<AppPackageListModel.AppPackageListModelItem>>(json!!.openStream()).map { it.packageX }
        // parse domain list
        rulePatternList = ruleList.lines().mapNotNull {
            // split with, and ignore blank
            runCatching { RulePattern.fromString(it.trim()) }.getOrNull()
        }
    }

    @Test
    fun rulePatternTest() {
        val packageList = packageList.filter { it.isNotBlank() }
        val matchedList = packageList.filter { packageName ->
            rulePatternList.any {
                it.match(packageName)
            }
        }
        println("total package: ${packageList.size}, matched package: ${matchedList.size}")
        assertTrue(matchedList.isNotEmpty())
    }
}