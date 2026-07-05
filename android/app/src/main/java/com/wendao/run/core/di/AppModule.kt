package com.wendao.run.core.di

import android.content.Context
import androidx.room.Room
import com.wendao.run.BuildConfig
import com.wendao.run.core.data.AuthRepository
import com.wendao.run.core.data.SessionStore
import com.wendao.run.core.database.PaoxiuDatabase
import com.wendao.run.core.database.dao.SyncQueueDao
import com.wendao.run.core.network.AuthApi
import com.wendao.run.core.network.AuthInterceptor
import com.wendao.run.core.network.SessionClearInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideSessionStore(@ApplicationContext context: Context): SessionStore =
        SessionStore(context)

    @Provides
    @Singleton
    fun provideOkHttpClient(sessionStore: SessionStore): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionStore))
            .addInterceptor(SessionClearInterceptor(sessionStore))
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(BuildConfig.API_BASE_URL))
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): com.wendao.run.core.network.UserApi =
        retrofit.create(com.wendao.run.core.network.UserApi::class.java)

    @Provides
    @Singleton
    fun provideSpiritRootApi(retrofit: Retrofit): com.wendao.run.core.network.SpiritRootApi =
        retrofit.create(com.wendao.run.core.network.SpiritRootApi::class.java)

    @Provides
    @Singleton
    fun provideGameApi(retrofit: Retrofit): com.wendao.run.core.network.GameApi =
        retrofit.create(com.wendao.run.core.network.GameApi::class.java)

    @Provides
    @Singleton
    fun provideGameRepository(
        gameApi: com.wendao.run.core.network.GameApi,
        authRepository: AuthRepository,
    ): com.wendao.run.core.data.GameRepository =
        com.wendao.run.core.data.GameRepository(gameApi, authRepository)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PaoxiuDatabase =
        Room.databaseBuilder(context, PaoxiuDatabase::class.java, "paoxiu.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideRunApi(retrofit: Retrofit): com.wendao.run.core.network.RunApi =
        retrofit.create(com.wendao.run.core.network.RunApi::class.java)

    @Provides
    @Singleton
    fun provideRunRepository(
        database: PaoxiuDatabase,
        syncQueueDao: SyncQueueDao,
        runApi: com.wendao.run.core.network.RunApi,
        sessionStore: SessionStore,
        json: Json,
    ): com.wendao.run.core.run.RunRepository = com.wendao.run.core.run.RunRepository(
        runDao = database.runDao(),
        syncQueueDao = syncQueueDao,
        runApi = runApi,
        sessionStore = sessionStore,
        json = json,
    )

    @Provides
    @Singleton
    fun provideSyncQueueDao(database: PaoxiuDatabase): SyncQueueDao =
        database.syncQueueDao()

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApi: AuthApi,
        sessionStore: SessionStore,
        userApi: com.wendao.run.core.network.UserApi,
        @ApplicationContext context: Context,
    ): AuthRepository = com.wendao.run.core.data.AuthRepository(
        context,
        authApi,
        sessionStore,
        userApi,
    )

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
