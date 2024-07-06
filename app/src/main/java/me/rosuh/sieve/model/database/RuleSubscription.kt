package me.rosuh.sieve.model.database

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import me.rosuh.sieve.model.RuleMode
import me.rosuh.sieve.model.SyncStatus
import java.util.Date

@Entity(
    indices = [
        Index("subscription_id")
    ]
)
data class RuleSubscription(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "subscription_id") val subscriptionId: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "mode") val ruleMode: RuleMode,
    @ColumnInfo(name = "enable") val enable: Boolean,
    @ColumnInfo(name = "priority") val priority: Int,
    @ColumnInfo(name = "create_time") val createTimeMill: Date,
    @ColumnInfo(name = "update_time") val updateTimeMill: Date,
    @ColumnInfo(name = "last_sync_time") val lastSyncTimeMill: Date,
    @ColumnInfo(name = "last_sync_status") val lastSyncStatus: SyncStatus,
    @ColumnInfo(name = "version") val version: Int,
    @ColumnInfo(name = "extra") val extra: String
)

@Entity(
    indices = [
        Index(value = ["rule_type", "rule_value"], unique = true)
    ]
)
data class Rule(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rule_id") val id: Int = 0,
    @ColumnInfo(name = "rule_type") val ruleType: String,
    @ColumnInfo(name = "rule_value") val ruleValue: String,
)

@Entity(
    primaryKeys = ["subscription_id", "rule_id"],
)
data class SubscriptionRuleCrossRef(
    @ColumnInfo(name = "subscription_id") val subscriptionId: Int,
    @ColumnInfo(name = "rule_id") val ruleId: Int
)

data class RuleSubscriptionWithRules(
    @Embedded val ruleSubscription: RuleSubscription,
    @Relation(
        parentColumn = "subscription_id",
        entityColumn = "rule_id",
        associateBy = Junction(SubscriptionRuleCrossRef::class)
    )
    val ruleList: List<Rule>
)

@Immutable
data class StableRuleSubscriptionWithRules(
    val list: List<RuleSubscriptionWithRules>
):List<RuleSubscriptionWithRules> by list {
    companion object {
        val empty = StableRuleSubscriptionWithRules(emptyList())
    }
}
