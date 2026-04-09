package com.example.musicapp.player.di

import com.example.musicapp.domain.player.PlaybackController
import com.example.musicapp.player.controller.Media3PlaybackController
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

