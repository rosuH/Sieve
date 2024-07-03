package me.rosuh.sieve.model.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [RuleSubscription::class, Rule::class, SubscriptionRuleCrossRef::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleSubscriptionDao(): SubscriptionDao
}