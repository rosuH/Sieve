package me.rosuh.sieve.model

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.collection.LruCache
import arrow.core.Either
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.decodeURLPart
import io.ktor.http.decodeURLQueryComponent
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json.Default.encodeToString
import me.rosuh.sieve.model.database.AppDatabase
import me.rosuh.sieve.model.database.Rule
import me.rosuh.sieve.model.database.RuleSubscriptionWithRules
import me.rosuh.sieve.utils.Logger
import me.rosuh.sieve.utils.catchIO
import okhttp3.HttpUrl
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.measureTime

@Singleton
class RuleRepo @Inject constructor(
    val db: AppDatabase?,
    val httpClient: HttpClient,
) {
    companion object {
        private const val TAG = "RuleRepo"
    }
    sealed class ExportType {
        abstract fun jump(context: Context)

        data object Surfboard : ExportType() {
            override fun jump(context: Context) {
                val packageName = "com.getsurfboard"
                val activityName = "com.getsurfboard.ui.activity.MainActivity"
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setComponent(ComponentName(packageName, activityName))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                kotlin.runCatching {
                    context.startActivity(intent)
                }
            }
        }
        data object NekoBoxAndroid : ExportType() {
            override fun jump(context: Context) {
                val packageName = "moe.nb4a"
                val activityName = "io.nekohasekai.sagernet.ui.MainActivity"
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setComponent(ComponentName(packageName, activityName))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                kotlin.runCatching {
                    context.startActivity(intent)
                }
            }
        }
        data object ClashMetaForAndroid : ExportType() {
            override fun jump(context: Context) {
                val packageName = "com.github.metacubex.clash.meta"
                val activityName = "com.github.kr328.clash.MainActivity"
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setComponent(ComponentName(packageName, activityName))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                kotlin.runCatching {
                    context.startActivity(intent)
                }
            }
        }

        data object FlClash : ExportType() {
            override fun jump(context: Context) {
                val packageName = "com.follow.clash"
                val activityName = "com.follow.clash.MainActivity"
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setComponent(ComponentName(packageName, activityName))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                kotlin.runCatching {
                    context.startActivity(intent)
                }
            }
        }
    }

    private val rulePatternCache by lazy { LruCache<Rule, RulePattern>(1000) }

    suspend fun downloadConf(
        name: String?,
        url: String,
        fileDir: File
    ): Either<Throwable, ConfParser> = catchIO {
        val response: HttpResponse = httpClient.get(url)
        val channel = response.bodyAsChannel()
        val confName = name.takeIf { it.isNullOrEmpty().not() } ?: File(URL(url.decodeURLPart().decodeURLQueryComponent()).path).name
        val fileName = response.headers["Content-Disposition"]
            ?.split(";")
            ?.find { it.contains("filename=") }
            ?.split("=")
            ?.get(1)
            ?.replace("\"", "")
            ?: runCatching { confName }.getOrNull() ?: name ?: "default.conf"
        val file = File(fileDir, fileName)
        val filePath = file.absolutePath
        val confParser = ConfParser(fileName, url, filePath = filePath, db?.ruleSubscriptionDao()?.getAll()?.size ?: 0)
        Logger.i(TAG, "downloadConf: $url, $filePath")
        file.writer(Charsets.UTF_8).use {
            while (channel.isClosedForRead.not()) {
                val line = channel.readUTF8Line(Int.MAX_VALUE)
                confParser.parse(line ?: "")
                it.write(line ?: "")
                Logger.d(TAG, "downloadConf: writing --> $line")
            }
        }
        return@catchIO confParser
    }

    private suspend fun syncConf(
        subscriptionWithRules: RuleSubscriptionWithRules,
        defaultFileDir: File
    ) = withContext(Dispatchers.IO) {
        val subscription = subscriptionWithRules.ruleSubscription
        val fileDir = File(subscription.filePath).parentFile ?: defaultFileDir
        val confParser = downloadConf(subscription.name, subscription.url, fileDir).fold(
            { throw it },
            { it }
        )
        val newConf = confParser.get()
        val newSubscription = newConf.ruleSubscription
        val ruleSubscription = subscriptionWithRules.copy(
            ruleSubscription = subscription.copy(
                name = newSubscription.name,
                url = newSubscription.url,
                ruleMode = newSubscription.ruleMode,
                updateTimeMill = newSubscription.updateTimeMill,
                lastSyncTimeMill = newSubscription.lastSyncTimeMill,
                lastSyncStatus = newSubscription.lastSyncStatus,
                version = newSubscription.version,
                extra = newSubscription.extra,
                filePath = newSubscription.filePath
            ),
            ruleList = newConf.ruleList
        )
        db?.ruleSubscriptionDao()?.insertAll(ruleSubscription)
        return@withContext
    }

    suspend fun syncAllConf(mode: RuleMode, defaultFileDir: File) = withContext(Dispatchers.IO) {
        val subscriptionList = db?.ruleSubscriptionDao()?.getAllActiveWithRule(mode) ?: emptyList()
        subscriptionList.forEach { subscription ->
            measureTime {
                syncConf(subscription, defaultFileDir)
            }.let {
                Logger.d(TAG, "syncConf ${subscription.ruleSubscription.url} cost: $it")
            }
        }
    }

    /**
     * surfbox
     * {
     *    "mode": "disallowed",
     *    "package_name": [
     *        "com.example.app"
     *    ]
     * }
     *
     * NekoBoxForAndroid
     *  bypass
     *      true com.example
     *  proxy
     *      false com.example
     */
    suspend fun exportApplicationList(
        packageList: List<AppInfo>,
        mode: RuleMode,
        type: RuleRepo.ExportType
    ): String = withContext(Dispatchers.Default) {
        val list = filter(packageList, mode)
        // export to specific format
        return@withContext when (type) {
            ExportType.Surfboard -> {
                // export to surfboard format
                val surfboardModel = SurfboardModel(
                    mode = when (mode) {
                        RuleMode.Proxy -> "allowed"
                        RuleMode.ByPass -> "disallowed"
                    },
                    packageName = list.map { it.packageName }
                )
                encodeToString(SurfboardModel.serializer(), surfboardModel)
            }

            ExportType.NekoBoxAndroid -> {
                val flag = when (mode) {
                    RuleMode.Proxy -> "false"
                    RuleMode.ByPass -> "true"
                }
                "$flag\n" + list.joinToString("\n") {
                    it.packageName
                }
            }

            ExportType.ClashMetaForAndroid -> {
                list.joinToString("\n") {
                    "$it.packageName"
                }
            }

            /**
             * bypass mode
             * ```
             * {
             *   "mode": "acceptSelected",
             *   "acceptList": [],
             *   "rejectList": ["com.tencent.mobileqq", "com.tencent.karaoke"],
             *   "sort": "none",
             *   "isFilterSystemApp": true
             * }
             *
             * ```
             * proxy mode
             * ```
             * {
             *   "mode": "rejectSelected",
             *   "acceptList": [],
             *   "rejectList": ["com.tencent.mobileqq", "com.tencent.karaoke"],
             *   "sort": "none",
             *   "isFilterSystemApp": true
             * }
             *
             * ```
             */

            /**
             * bypass mode
             * ```
             * {
             *   "mode": "acceptSelected",
             *   "acceptList": [],
             *   "rejectList": ["com.tencent.mobileqq", "com.tencent.karaoke"],
             *   "sort": "none",
             *   "isFilterSystemApp": true
             * }
             *
             * ```
             * proxy mode
             * ```
             * {
             *   "mode": "rejectSelected",
             *   "acceptList": [],
             *   "rejectList": ["com.tencent.mobileqq", "com.tencent.karaoke"],
             *   "sort": "none",
             *   "isFilterSystemApp": true
             * }
             *
             * ```
             */
            ExportType.FlClash -> {
                val resultList = list.map { it.packageName }
                val flClashModel = FlClashModel(
                    mode = when (mode) {
                        RuleMode.Proxy -> "rejectSelected"
                        RuleMode.ByPass -> "acceptSelected"
                    },
                    acceptList = (if (mode == RuleMode.Proxy) emptyList() else resultList).toImmutableList(),
                    rejectList = (if (mode == RuleMode.Proxy) resultList else emptyList()).toImmutableList(),
                    sort = "none",
                    isFilterSystemApp = true
                )
                encodeToString(FlClashModel.serializer(), flClashModel)
            }
        }
    }

    suspend fun filter(packageList: List<AppInfo>, mode: RuleMode): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val subscriptionList =
                db?.ruleSubscriptionDao()?.getAllActiveWithRule(mode) ?: emptyList()
            return@withContext packageList.filter { appInfo ->
                var match = false
                subscriptionList.forEach sub@{ subscription ->
                    subscription.ruleList.forEach { rule ->
                        val pattern = rulePatternCache[rule] ?: RulePattern.fromRule(rule)
                            .also { rulePatternCache.put(rule, it) }
                        if (pattern.match(appInfo.packageName)) {
                            match = true
                            return@sub
                        }
                    }
                }
                match
            }
        }

    suspend fun getAllActiveWithRule(mode: RuleMode): List<RuleSubscriptionWithRules> = withContext(Dispatchers.IO) {
        return@withContext db?.ruleSubscriptionDao()?.getAllActiveWithRule(mode) ?: throw IllegalStateException(
            "db is null"
        )
    }

    fun getAllActiveWithRuleFlow(): Flow<List<RuleSubscriptionWithRules>> {
        return db?.ruleSubscriptionDao()?.getAllActiveWithRuleFlow() ?: throw IllegalStateException(
            "db is null"
        )
    }

    fun getAllActiveWithRuleFlow(mode: RuleMode): Flow<List<RuleSubscriptionWithRules>> {
        return db?.ruleSubscriptionDao()?.getAllWithRuleFlowByMode(mode)
            ?: throw IllegalStateException(
                "db is null"
            )
    }

    suspend fun addSubscription(name: String?, url: HttpUrl, fileDir: File): Unit = withContext(Dispatchers.IO) {
        downloadConf(name, url.toString(), fileDir).fold(
            { throw it },
            { confParser ->
                val ruleSubscription = confParser.get()
                db?.ruleSubscriptionDao()?.insertAll(ruleSubscription)
            })
    }

    suspend fun switchSubscription(subscription: RuleSubscriptionWithRules, checked: Boolean) =
        withContext(Dispatchers.IO) {
            db?.ruleSubscriptionDao()?.update(subscription.ruleSubscription.copy(enable = checked))
        }

    suspend fun deleteSubscription(subscription: RuleSubscriptionWithRules) {
        db?.ruleSubscriptionDao()?.delete(subscription)
    }
}