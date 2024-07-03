package me.rosuh.sieve.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.Charsets
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.json.Json
import me.rosuh.sieve.model.database.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext app: Context
    ): AppDatabase? {
        val builder = Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "database"
        )
        try {
            return builder.build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @Singleton
    @Provides
    fun provideRuleSubscriptionDao(db: AppDatabase) = db.ruleSubscriptionDao()

    @Provides
    fun provideHttpEngine(): HttpClientEngine = OkHttp.create()

    @Singleton
    @Provides
    fun provideHttpClient(engine: HttpClientEngine) = HttpClient(engine) {
        install(Logging)
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        install(HttpCache)
        Charsets {
            // Allow using `UTF_8`.
            register(Charsets.UTF_8)

            // Allow using `ISO_8859_1` with quality 0.1.
            register(Charsets.ISO_8859_1, quality=0.1f)

            // Specify Charset to send request(if no charset in request headers).
            sendCharset = Charsets.UTF_8

            // Specify Charset to receive response(if no charset in response headers).
            responseCharsetFallback = Charsets.UTF_8
        }
    }
}