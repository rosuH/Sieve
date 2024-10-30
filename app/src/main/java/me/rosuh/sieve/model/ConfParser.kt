package me.rosuh.sieve.model

import me.rosuh.sieve.model.database.Rule
import me.rosuh.sieve.model.database.RuleSubscription
import me.rosuh.sieve.model.database.RuleSubscriptionWithRules
import java.io.File
import java.util.Date

/**
 * parse conf file
 * ```
 * [config]
 * mode=bypass
 * url=https://example.com/example.conf
 * [rule]
 * REGEX,com.example.*
 * EXTRA,com.example.1
 * ```
 */
class ConfParser(
    val name: String,
    val url: String,
    val filePath: String,
    val priority: Int = 0
) {
    private val ruleList: MutableList<Rule> = mutableListOf()

    private lateinit var mode: RuleMode

    fun get(): RuleSubscriptionWithRules {
        val subscription = RuleSubscription(
            subscriptionId = 0,
            name = name,
            url = url,
            ruleMode = mode,
            enable = true,
            priority = priority,
            createTimeMill = Date(System.currentTimeMillis()),
            updateTimeMill = Date(System.currentTimeMillis()),
            lastSyncTimeMill = Date(System.currentTimeMillis()),
            lastSyncStatus = SyncStatus.Success,
            version = 0,
            extra = "",
            filePath = filePath
        )
        return RuleSubscriptionWithRules(subscription, ruleList)
    }

    fun parse(string: String) {
        when (val rule = RulePattern.fromString(string)) {
            is RulePattern.Regex, is RulePattern.Extra -> {
                ruleList.add(RulePattern.toRule(rule))
            }

            is RulePattern.Mode -> {
                mode = when (rule.value) {
                    "proxy" -> RuleMode.Proxy
                    "bypass" -> RuleMode.ByPass
                    else -> throw IllegalArgumentException("unknown mode: ${rule.value}")
                }
            }

            is RulePattern.Config -> {}

            is RulePattern.Rule -> {}

            is RulePattern.Unknown -> {}
        }
    }
}