package com.example.musicapp.data.di

import com.example.musicapp.core.DefaultDispatchersProvider
import com.example.musicapp.core.DispatchersProvider
import com.example.musicapp.core.PlaybackTokenSource
import com.example.musicapp.data.auth.SessionPlaybackTokenSource
import com.example.musicapp.data.local.LocalMusicDataSource
import com.example.musicapp.data.local.MediaStoreLocalMusicDataSource
import com.example.musicapp.data.repository.RemoteDriveRepository
import com.example.musicapp.data.repository.RemoteFavoritesRepository
import com.example.musicapp.data.repository.RemoteMusicRepository
import com.example.musicapp.data.repository.RemotePlaylistRepository
import com.example.musicapp.data.repository.DefaultSettingsRepository
import com.example.musicapp.domain.repository.DriveRepository
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
    abstract fun bindDriveRepository(
        implementation: RemoteDriveRepository,
    ): DriveRepository

    @Binds
    abstract fun bindMusicRepository(
        implementation: RemoteMusicRepository,
    ): MusicRepository

    @Binds
    abstract fun bindFavoritesRepository(
        implementation: RemoteFavoritesRepository,
    ): FavoritesRepository

    @Binds
    abstract fun bindPlaylistRepository(
        implementation: RemotePlaylistRepository,
    ): PlaylistRepository

    @Binds
    abstract fun bindSettingsRepository(
        implementation: DefaultSettingsRepository,
    ): SettingsRepository

    @Binds
    abstract fun bindPlaybackTokenSource(
        implementation: SessionPlaybackTokenSource,
    ): PlaybackTokenSource
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDispatchersProvider(): DispatchersProvider = DefaultDispatchersProvider
}
