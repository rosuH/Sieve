package me.rosuh.sieve.model.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.rosuh.sieve.model.RuleMode

@Dao
interface SubscriptionDao {

    @Transaction
    @Query("SELECT * FROM RuleSubscription ORDER BY priority ASC")
    suspend fun getAll(): List<RuleSubscriptionWithRules>

    @Transaction
    @Query("SELECT * FROM RuleSubscription WHERE mode = :mode ORDER BY priority ASC")
    suspend fun getAll(mode: RuleMode): List<RuleSubscriptionWithRules>

    @Transaction
    @Query("SELECT * FROM RuleSubscription WHERE mode = :mode AND enable = 1 ORDER BY priority ASC")
    suspend fun getAllActive(mode: RuleMode): List<RuleSubscriptionWithRules>

    @Transaction
    @Query("SELECT * FROM RuleSubscription WHERE mode = :mode AND enable = 1 ORDER BY priority ASC")
    suspend fun getAllActiveWithRule(mode: RuleMode): List<RuleSubscriptionWithRules>

    @Transaction
    @Query("SELECT * FROM RuleSubscription WHERE enable = 1 ORDER BY priority ASC")
    fun getAllActiveWithRuleFlow(): Flow<List<RuleSubscriptionWithRules>>

    @Transaction
    @Query("SELECT * FROM RuleSubscription WHERE mode = :mode ORDER BY priority ASC")
    fun getAllWithRuleFlowByMode(mode: RuleMode): Flow<List<RuleSubscriptionWithRules>>

    /**
     * find subscription by id
     */
    @Query("SELECT * FROM RuleSubscription WHERE subscription_id = :id")
    suspend fun findById(id: Int): RuleSubscription?

    /**
     * update subscription
     */
    @Transaction
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(subscription: RuleSubscription)

    /**
     * insert
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: RuleSubscription): Long

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRule(rule: List<Rule>): Array<Long>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRule(rule: Rule): Long

    // find Rule
    @Query("SELECT * FROM Rule WHERE rule_type = :type AND rule_value = :value")
    suspend fun findRuleByTypeAndValue(type: String, value: String): Rule?

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptionRuleCrossRef(subscriptionRuleCrossRef: SubscriptionRuleCrossRef): Long

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubscriptionRuleCrossRef(subscriptionRuleCrossRef: List<SubscriptionRuleCrossRef>): Array<Long>


    @Transaction
    open suspend fun insertAll(subscription: RuleSubscriptionWithRules) {
        val subscriptionId = insertSubscription(subscription.ruleSubscription)
        val ruleIds = List(subscription.ruleList.size) { index ->
            findRuleByTypeAndValue(
                subscription.ruleList[index].ruleType,
                subscription.ruleList[index].ruleValue
            )?.id ?: insertRule(subscription.ruleList[index])
        }
        val subscriptionRuleCrossRef = List(subscription.ruleList.size) { index ->
            SubscriptionRuleCrossRef(subscriptionId.toInt(), ruleIds[index].toInt())
        }
        insertSubscriptionRuleCrossRef(subscriptionRuleCrossRef)
    }

    @Transaction
    @Delete
    suspend fun deleteSubscription(subscription: RuleSubscription)

    @Transaction
    @Delete
    suspend fun deleteRule(rule: List<Rule>)

    @Query("DELETE FROM SubscriptionRuleCrossRef WHERE subscription_id = :subscriptionId")
    suspend fun deleteCrossRefBySubscriptionId(subscriptionId: Int)

    @Query("SELECT COUNT(*) FROM SubscriptionRuleCrossRef WHERE rule_id = :ruleId")
    suspend fun countSubscriptionsForRule(ruleId: Int): Int


    /**
     * delete
     */
    @Transaction
    suspend fun delete(subscription: RuleSubscriptionWithRules) {
        // 先删除交叉引用
        deleteCrossRefBySubscriptionId(subscription.ruleSubscription.subscriptionId)

        // 删除规则时先检查是否有其他订阅引用
        for (rule in subscription.ruleList) {
            val count = countSubscriptionsForRule(rule.id)
            if (count == 0) {
                deleteRule(listOf(rule)) // 仅在没有引用时删除
            }
        }

        // 删除订阅
        deleteSubscription(subscription.ruleSubscription)
    }
}