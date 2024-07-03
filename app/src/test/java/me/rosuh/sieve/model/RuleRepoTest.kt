package me.rosuh.sieve.model

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import me.rosuh.sieve.di.AppModule
import org.junit.Assert.*
import org.junit.Before

import org.junit.Test

class RuleRepoTest {

    private lateinit var repo: RuleRepo

    @Before
    fun setUp() {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""
                    [config]
                    mode=bypass
                    url=https://gist.githubusercontent.com/rosuH/a6a51296c5e24fac25e80e0a3e16cac3/raw/de77c5f739bb6853995f646452028d524aae65ec/sample.conf

                    [rule]
                    REGEX,com.baidu.*
                    REGEX,com.chinamworld.*
                    REGEX,com.ss.*
                    REGEX,com.taobao.*
                    REGEX,com.tencent.*
                    EXTRA,com.eg.android.AlipayGphone
                    EXTRA,com.xunmeng.pinduoduo
                    EXTRA,com.smile.gifmaker
                    EXTRA,com.sankuai.meituan
                    EXTRA,com.kuaishou.nebula
                    EXTRA,com.autonavi.minimap
                    EXTRA,com.xingin.xhs
                    EXTRA,com.jingdong.app.mall
                    EXTRA,com.dragon.read
                    EXTRA,com.alibaba.android.rimet
                    EXTRA,tv.danmaku.bili
                    EXTRA,com.sina.weibo
                    EXTRA,com.kugou.android
                    EXTRA,com.android.bankabc
                    EXTRA,com.UCMobile
                    EXTRA,cn.xuexi.android
                    EXTRA,com.qiyi.video
                    EXTRA,com.tmri.app.main
                    EXTRA,com.icbc
                    EXTRA,com.xs.fm
                    EXTRA,cn.wps.moffice_eng
                    EXTRA,com.quark.browser
                    EXTRA,com.netease.cloudmusic
                    EXTRA,com.moji.mjweather
                    EXTRA,com.youku.phone
                    EXTRA,com.ximalaya.ting.android
                    EXTRA,com.kmxs.reader
                    EXTRA,com.lemon.lv
                    EXTRA,com.MobileTicket
                    EXTRA,com.achievo.vipshop
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain;charset=utf-8")
            )
        }
        repo = RuleRepo(null, AppModule.provideHttpClient(mockEngine))
    }

    @Test
    fun downloadConf() {
        runBlocking {
            repo.downloadConf("test", "https://gist.githubusercontent.com/rosuH/a6a51296c5e24fac25e80e0a3e16cac3/raw/de77c5f739bb6853995f646452028d524aae65ec/sample.conf")
                .fold(
                    { fail(it.message) },
                    { confParser ->
                        val ruleSubscription = confParser.get()
                        assertEquals("sample.conf", ruleSubscription.ruleSubscription.name, )
                        assertEquals(35, ruleSubscription.ruleList.size, )
                    })
        }
    }
}