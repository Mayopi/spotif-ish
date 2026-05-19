package com.example.spotifish.data.di

import com.example.spotifish.core.DefaultDispatchersProvider
import com.example.spotifish.core.DispatchersProvider
import com.example.spotifish.core.PlaybackTokenSource
import com.example.spotifish.data.auth.SessionPlaybackTokenSource
import com.example.spotifish.data.local.LocalMusicDataSource
import com.example.spotifish.data.local.MediaStoreLocalMusicDataSource
import com.example.spotifish.data.repository.RemoteDriveRepository
import com.example.spotifish.data.repository.RemoteFavoritesRepository
import com.example.spotifish.data.repository.RemoteMusicRepository
import com.example.spotifish.data.repository.RemotePlaylistRepository
import com.example.spotifish.data.repository.DefaultSettingsRepository
import com.example.spotifish.domain.repository.DriveRepository
import com.example.spotifish.domain.repository.FavoritesRepository
import com.example.spotifish.domain.repository.MusicRepository
import com.example.spotifish.domain.repository.PlaylistRepository
import com.example.spotifish.domain.repository.SettingsRepository
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
