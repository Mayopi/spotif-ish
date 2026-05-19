package com.example.spotifish.data.network

import com.example.spotifish.data.network.dto.AddSongRequest
import com.example.spotifish.data.network.dto.AlbumGroupDto
import com.example.spotifish.data.network.dto.ArtistGroupDto
import com.example.spotifish.data.network.dto.AuthTokenPair
import com.example.spotifish.data.network.dto.ConnectDriveRequest
import com.example.spotifish.data.network.dto.CreatePlaylistRequest
import com.example.spotifish.data.network.dto.DriveFolderListDto
import com.example.spotifish.data.network.dto.FavoritesResponseDto
import com.example.spotifish.data.network.dto.GoogleSignInRequest
import com.example.spotifish.data.network.dto.HomeResponseDto
import com.example.spotifish.data.network.dto.PlaybackEventRequest
import com.example.spotifish.data.network.dto.PlaylistDto
import com.example.spotifish.data.network.dto.PlaylistListResponseDto
import com.example.spotifish.data.network.dto.RefreshTokenRequest
import com.example.spotifish.data.network.dto.RefreshedTokenPair
import com.example.spotifish.data.network.dto.RenamePlaylistRequest
import com.example.spotifish.data.network.dto.SignOutRequest
import com.example.spotifish.data.network.dto.ReorderSongsRequest
import com.example.spotifish.data.network.dto.SetDriveFolderRequest
import com.example.spotifish.data.network.dto.SongDto
import com.example.spotifish.data.network.dto.SongPageDto
import com.example.spotifish.data.network.dto.SyncRunResponse
import com.example.spotifish.data.network.dto.SyncStatusDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The full HTTP contract that the Spotifish backend serves. Endpoint paths and shapes
 * mirror BACKEND_PRD.md section 9 (API Contract) at the repo root.
 *
 * Authentication is handled by [AuthInterceptor], which attaches `Authorization:
 * Bearer <jwt>` to every request and transparently refreshes on 401. The two
 * endpoints under `v1/auth` are excluded from interception via [NoAuth].
 */
interface SpotifishApi {

    // -------------------- Auth --------------------

    @NoAuth
    @POST("v1/auth/google")
    suspend fun signInWithGoogle(@Body request: GoogleSignInRequest): AuthTokenPair

    @NoAuth
    @POST("v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): RefreshedTokenPair

    @POST("v1/auth/sign-out")
    suspend fun signOut(@Body request: SignOutRequest)

    // -------------------- Drive connection --------------------

    @POST("v1/drive/connect")
    suspend fun connectDrive(@Body request: ConnectDriveRequest)

    @GET("v1/drive/folders")
    suspend fun listDriveFolders(
        @Query("parentId") parentId: String? = null,
    ): DriveFolderListDto

    @POST("v1/drive/connection")
    suspend fun setDriveConnection(@Body request: SetDriveFolderRequest)

    @DELETE("v1/drive/connection")
    suspend fun disconnectDrive()

    // -------------------- Sync --------------------

    @POST("v1/sync/run")
    suspend fun runSync(): SyncRunResponse

    @POST("v1/sync/pause")
    suspend fun pauseSync(): SyncRunResponse

    @POST("v1/sync/resume")
    suspend fun resumeSync(): SyncRunResponse

    @GET("v1/sync/status")
    suspend fun syncStatus(): SyncStatusDto

    // -------------------- Library --------------------

    @GET("v1/songs")
    suspend fun listSongs(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 100,
    ): SongPageDto

    @GET("v1/songs/{id}")
    suspend fun getSong(@Path("id") songId: String): SongDto

    @GET("v1/songs/search")
    suspend fun searchSongs(@Query("q") query: String): SongPageDto

    @GET("v1/home")
    suspend fun home(): HomeResponseDto

    @GET("v1/artists")
    suspend fun listArtists(): List<ArtistGroupDto>

    @GET("v1/albums")
    suspend fun listAlbums(): List<AlbumGroupDto>

    // -------------------- Playlists --------------------

    @GET("v1/playlists")
    suspend fun listPlaylists(): PlaylistListResponseDto

    @POST("v1/playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistRequest): PlaylistDto

    @PATCH("v1/playlists/{id}")
    suspend fun renamePlaylist(
        @Path("id") playlistId: String,
        @Body request: RenamePlaylistRequest,
    )

    @DELETE("v1/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") playlistId: String)

    @POST("v1/playlists/{id}/songs")
    suspend fun addSongToPlaylist(
        @Path("id") playlistId: String,
        @Body request: AddSongRequest,
    )

    @DELETE("v1/playlists/{id}/songs/{songId}")
    suspend fun removeSongFromPlaylist(
        @Path("id") playlistId: String,
        @Path("songId") songId: String,
    )

    @PUT("v1/playlists/{id}/songs")
    suspend fun reorderPlaylistSongs(
        @Path("id") playlistId: String,
        @Body request: ReorderSongsRequest,
    )

    // -------------------- Playback events --------------------

    @POST("v1/playback/events")
    suspend fun recordPlaybackEvent(@Body request: PlaybackEventRequest)

    @GET("v1/playback/recent")
    suspend fun listRecentlyPlayed(@Query("limit") limit: Int = 20): SongPageDto

    // -------------------- Favorites --------------------

    @GET("v1/favorites")
    suspend fun listFavorites(): FavoritesResponseDto

    @PUT("v1/favorites/{songId}")
    suspend fun likeSong(@Path("songId") songId: String)

    @DELETE("v1/favorites/{songId}")
    suspend fun unlikeSong(@Path("songId") songId: String)
}
