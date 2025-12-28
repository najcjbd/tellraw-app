package com.tellraw.app.di

import android.content.Context
import androidx.room.Room
import com.tellraw.app.data.local.AppDatabase
import com.tellraw.app.data.remote.GithubApiService
import com.tellraw.app.data.repository.TellrawRepository
import com.tellraw.app.data.repository.VersionCheckRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideGithubRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideGithubApiService(githubRetrofit: Retrofit): GithubApiService {
        return githubRetrofit.create(GithubApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tellraw_database"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideTellrawRepository(
        database: AppDatabase
    ): TellrawRepository {
        return TellrawRepository(database)
    }
    
    @Provides
    @Singleton
    fun provideVersionCheckRepository(
        githubApiService: GithubApiService,
        @ApplicationContext context: Context
    ): VersionCheckRepository {
        return VersionCheckRepository(githubApiService, context)
    }
}