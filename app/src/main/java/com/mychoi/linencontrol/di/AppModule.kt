package com.mychoi.linencontrol.di

import android.content.Context
import androidx.room.Room
import com.mychoi.linencontrol.BuildConfig
import com.mychoi.linencontrol.data.local.LinenDatabase
import com.mychoi.linencontrol.data.local.dao.StockSaveDao
import com.mychoi.linencontrol.data.remote.api.ClaudeApiService
import com.mychoi.linencontrol.data.remote.repository.ClaudeRepository
import com.mychoi.linencontrol.data.repository.StockRepositoryImpl
import com.mychoi.linencontrol.domain.repository.StockRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideStockRepository(): StockRepository = StockRepositoryImpl()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-api-key", BuildConfig.CLAUDE_API_KEY)
                    .addHeader("anthropic-version", "2023-06-01")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideClaudeApiService(retrofit: Retrofit): ClaudeApiService =
        retrofit.create(ClaudeApiService::class.java)

    @Provides
    @Singleton
    fun provideClaudeRepository(apiService: ClaudeApiService): ClaudeRepository =
        ClaudeRepository(apiService)

    @Provides
    @Singleton
    fun provideLinenDatabase(@ApplicationContext context: Context): LinenDatabase =
        Room.databaseBuilder(context, LinenDatabase::class.java, "linen_db").build()

    @Provides
    @Singleton
    fun provideStockSaveDao(db: LinenDatabase): StockSaveDao = db.stockSaveDao()
}