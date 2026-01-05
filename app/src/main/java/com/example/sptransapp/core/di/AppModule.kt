package com.example.sptransapp.core.di

import android.content.Context
import androidx.room.Room
import com.example.sptransapp.data.api.AuthInterceptor
import com.example.sptransapp.data.api.SPTransApi
import com.example.sptransapp.data.database.AppDatabase
import com.example.sptransapp.data.database.dao.CorridorDao
import com.example.sptransapp.data.database.dao.LineDao
import com.example.sptransapp.data.database.dao.StopDao
import com.example.sptransapp.data.repository.BusRepositoryImpl
import com.example.sptransapp.domain.repository.BusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
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
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                "sptrans_db",
            ).build()

    @Provides
    fun provideLineDao(database: AppDatabase): LineDao = database.lineDao()

    @Provides
    fun provideStopDao(database: AppDatabase): StopDao = database.stopDao()

    @Provides
    fun provideCorridorDao(database: AppDatabase): CorridorDao = database.corridorDao()

    @Provides
    @Singleton
    fun provideCookieJar(): CookieJar =
        object : CookieJar {
            private val cookieStore = HashMap<String, List<Cookie>>()

            override fun saveFromResponse(
                url: HttpUrl,
                cookies: List<Cookie>,
            ) {
                cookieStore[url.host] = cookies
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> = cookieStore[url.host] ?: ArrayList()
        }

    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor = AuthInterceptor()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: CookieJar,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .cookieJar(cookieJar)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("http://api.olhovivo.sptrans.com.br/v2.1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideSPTransApi(retrofit: Retrofit): SPTransApi = retrofit.create(SPTransApi::class.java)

    @Provides
    @Singleton
    fun provideBusRepository(
        api: SPTransApi,
        lineDao: LineDao,
        stopDao: StopDao,
        corridorDao: CorridorDao,
        @ApplicationContext context: Context,
    ): BusRepository =
        BusRepositoryImpl(
            api = api,
            lineDao = lineDao,
            stopDao = stopDao,
            corridorDao = corridorDao,
            context = context,
        )
}
