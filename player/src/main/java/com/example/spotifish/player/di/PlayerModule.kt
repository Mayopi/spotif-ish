package com.example.spotifish.player.di

import com.example.spotifish.domain.player.PlaybackController
import com.example.spotifish.player.controller.Media3PlaybackController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    abstract fun bindPlaybackController(
        implementation: Media3PlaybackController,
    ): PlaybackController
}

