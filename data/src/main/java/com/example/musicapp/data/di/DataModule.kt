package com.example.musicapp.data.di

import com.example.musicapp.core.DefaultDispatchersProvider
import com.example.musicapp.core.DispatchersProvider
import com.example.musicapp.data.drive.DriveMusicDataSource
import com.example.musicapp.data.drive.StubDriveMusicDataSource
import com.example.musicapp.data.local.LocalMusicDataSource
import com.example.musicapp.data.local.MediaStoreLocalMusicDataSource
import com.example.musicapp.data.repository.DefaultFavoritesRepository
import com.example.musicapp.data.repository.DefaultMusicRepository
import com.example.musicapp.data.repository.DefaultPlaylistRepository
import com.example.musicapp.data.repository.DefaultSettingsRepository
import com.example.musicapp.domain.repository.FavoritesRepository
import com.example.musicapp.domain.repository.MusicRepository
import com.example.musicapp.domain.repository.PlaylistRepository
import com.example.musicapp.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {

    @Binds
    abstract fun bindLocalMusicDataSource(
        implementation: MediaStoreLocalMusicDataSource,
    ): LocalMusicDataSource

    @Binds
    abstract fun bindDriveMusicDataSource(
        implementation: StubDriveMusicDataSource,
    ): DriveMusicDataSource

    @Binds
    abstract fun bindMusicRepository(
        implementation: DefaultMusicRepository,
    ): MusicRepository

    @Binds
    abstract fun bindFavoritesRepository(
        implementation: DefaultFavoritesRepository,
    ): FavoritesRepository

    @Binds
    abstract fun bindPlaylistRepository(
        implementation: DefaultPlaylistRepository,
    ): PlaylistRepository

    @Binds
    abstract fun bindSettingsRepository(
        implementation: DefaultSettingsRepository,
    ): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDispatchersProvider(): DispatchersProvider = DefaultDispatchersProvider
}

