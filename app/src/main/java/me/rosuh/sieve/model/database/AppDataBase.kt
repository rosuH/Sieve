package me.rosuh.sieve.model.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [RuleSubscription::class, Rule::class, SubscriptionRuleCrossRef::class], version = 2, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleSubscriptionDao(): SubscriptionDao
}


val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE RuleSubscription ADD COLUMN file_path TEXT NOT NULL DEFAULT ''")
    }
}