package me.rosuh.sieve.model

import android.content.pm.ApplicationInfo
import androidx.collection.LruCache
import arrow.core.Either
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.decodeURLPart
import io.ktor.http.decodeURLQueryComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.encodeToString
import me.rosuh.sieve.MainViewModel
import me.rosuh.sieve.model.database.AppDatabase
import me.rosuh.sieve.model.database.Rule
import me.rosuh.sieve.model.database.RuleSubscriptionWithRules
import me.rosuh.sieve.utils.catchIO
import okhttp3.HttpUrl
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepo @Inject constructor(
    val db: AppDatabase?,
    val httpClient: HttpClient,
) {
    sealed class ExportType {
        data object Surfboard : ExportType()
        data object NekoBoxAndroid : ExportType()
    }

    private val rulePatternCache by lazy { LruCache<Rule, RulePattern>(1000) }

    suspend fun downloadConf(
        name: String?,
        url: String,
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
        val confParser = ConfParser(fileName, url, db?.ruleSubscriptionDao()?.getAll()?.size ?: 0)
        while (channel.isClosedForRead.not()) {
            val line = channel.readUTF8Line(Int.MAX_VALUE)
            confParser.parse(line ?: "")
        }
        return@catchIO confParser
    }

    suspend fun syncConf(
        subscriptionWithRules: RuleSubscriptionWithRules
    ) = withContext(Dispatchers.IO) {
        val subscription = subscriptionWithRules.ruleSubscription
        val confParser = downloadConf(subscription.name, subscription.url).fold(
            { throw it },
            { it }
        )
        val newConf = confParser.get()
        val newSubscription = newConf.ruleSubscription
        val newList = newConf.ruleList
        val ruleSubscription = subscriptionWithRules.copy(
            ruleSubscription = subscription.copy(
                name = newSubscription.name,
                url = newSubscription.url,
                ruleMode = newSubscription.ruleMode,
                updateTimeMill = newSubscription.updateTimeMill,
                lastSyncTimeMill = newSubscription.lastSyncTimeMill,
                lastSyncStatus = newSubscription.lastSyncStatus,
                version = newSubscription.version,
                extra = newSubscription.extra
            ),
            ruleList = newConf.ruleList
        )
        db?.ruleSubscriptionDao()?.insertAll(ruleSubscription)
        return@withContext
    }

    suspend fun syncAllConf(mode: RuleMode) = withContext(Dispatchers.IO) {
        val subscriptionList = db?.ruleSubscriptionDao()?.getAllActiveWithRule(mode) ?: emptyList()
        subscriptionList.forEach { subscription ->
            syncConf(subscription)
        }
        return@withContext
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
    ): String {
        val list = filter(packageList, mode)
        // export to specific format
        return when (type) {
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

    suspend fun addSubscription(name: String?, url: HttpUrl): Unit = withContext(Dispatchers.IO) {
        downloadConf(name, url.toString()).fold(
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
}