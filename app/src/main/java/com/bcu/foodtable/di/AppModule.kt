package com.bcu.foodtable.di


import com.bcu.foodtable.AI.OpenAIClient
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
    fun provideOpenAIClient(): OpenAIClient {
        return OpenAIClient()
    }
}
