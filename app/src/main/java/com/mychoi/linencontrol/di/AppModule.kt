package com.mychoi.linencontrol.di

import com.mychoi.linencontrol.data.repository.StockRepositoryImpl
import com.mychoi.linencontrol.domain.repository.StockRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideStockRepository(): StockRepository {
        return StockRepositoryImpl()
    }
}