package me.rosuh.sieve.model

enum class RuleMode(val value: String) {
    Proxy("代理"), ByPass("绕过")
}

enum class SyncStatus {
    Success, Failed
}