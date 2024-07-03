package me.rosuh.sieve.model

import me.rosuh.sieve.model.database.Rule


sealed class RulePattern {

    abstract val value: String

    abstract val key: String

    abstract fun match(packageName: String): Boolean

    companion object {
        fun fromString(rule: String): RulePattern {
            return when  {
                rule.startsWith("[config]") -> Config(rule)
                rule.startsWith("[rule]") -> Rule(rule)
                rule.startsWith("mode=") -> {
                    val mode = rule.split("=")[1]
                    Mode(mode)
                }
                rule.startsWith("url=") -> {
                    val url = rule.split("=")[1]
                    Config(url)
                }
                rule.startsWith("EXTRA,") || rule.startsWith("REGEX,") -> {
                    val (key, value) = if (rule.contains(",")) {
                        rule.split(",")
                    } else {
                        listOf("UNKNOWN", rule)
                    }
                    when (key) {
                        "EXTRA" -> Extra(value)
                        "REGEX" -> Regex(value)
                        else -> Unknown(rule)
                    }

                }
                else -> Unknown(rule)
            }
        }

        fun fromRule(rule: me.rosuh.sieve.model.database.Rule): RulePattern {
            return when(rule.ruleType)  {
                "[config]" -> Config(rule.ruleValue)
                "[rule]" -> Rule(rule.ruleValue)
                "mode=" -> {
                    Mode(rule.ruleValue)
                }
                "EXTRA" -> Extra(rule.ruleValue)
                "REGEX" -> Regex(rule.ruleValue)
                else -> Unknown(rule.ruleValue)
            }
        }

        fun toRule(rulePattern: RulePattern): me.rosuh.sieve.model.database.Rule {
            return when(rulePattern)  {
                is Config -> Rule(ruleType = rulePattern.key, ruleValue = rulePattern.value)
                is Rule -> Rule(ruleType = rulePattern.key, ruleValue = rulePattern.value)
                is Mode -> Rule(ruleType = rulePattern.key, ruleValue = rulePattern.value)
                is Extra -> Rule(ruleType = rulePattern.key, ruleValue = rulePattern.value)
                is Regex -> Rule(ruleType = rulePattern.key, ruleValue = rulePattern.value)
                is Unknown -> Rule(ruleType = rulePattern.key, ruleValue = rulePattern.value)
            }
        }
    }

    data class Mode(
        override val value: String,
        override val key: String = "mode="
    ) : RulePattern() {
        override fun match(packageName: String): Boolean {
            return false
        }
    }

    data class Config(
        override val value: String,
        override val key: String = "[config]"
    ) : RulePattern() {
        override fun match(packageName: String): Boolean {
            return packageName.startsWith(key)
        }
    }

    data class Rule(
        override val value: String,
        override val key: String = "[rule]"
    ) : RulePattern() {
        override fun match(packageName: String): Boolean {
            return packageName.startsWith(key)
        }
    }

    data class Unknown(
        override val value: String,
        override val key: String = "UNKNOWN"
    ) : RulePattern() {
        override fun match(packageName: String): Boolean {
            return false
        }
    }

    data class Extra(
        override val value: String,
        override val key: String = "EXTRA"
    ) : RulePattern() {
        override fun match(packageName: String): Boolean {
            return packageName == value
        }
    }

    /**
     * match base url
     */
    data class Regex(
        override val value: String,
        override val key: String = "REGEX"
    ) : RulePattern() {
        override fun match(packageName: String): Boolean {
            return packageName.contains(value.toRegex())
        }
    }
}