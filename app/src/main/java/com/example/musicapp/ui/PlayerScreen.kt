package com.example.musicapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.ui.player.PlayerUiState

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val ambient = state.currentSong?.let { gradientForSong(it.id) }
        ?: Brush.verticalGradient(listOf(SpotifyCard, SpotifyBackground))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambient),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, SpotifyBackground),
                        startY = 200f,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "NOW PLAYING",
                    color = SpotifyWhite,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            val song = state.currentSong
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (song != null) {
                    ArtworkThumb(
                        song = song,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .shadow(24.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpotifyCard),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No song loaded",
                            color = SpotifyTextMuted,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.currentSongTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = SpotifyWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.currentSongArtist.ifBlank { "Unknown artist" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = SpotifyTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    enabled = state.currentSong != null,
                ) {
                    Icon(
                        imageVector = if (state.isCurrentSongFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = if (state.isCurrentSongFavorite) "Unlike" else "Like",
                        tint = if (state.isCurrentSongFavorite) SpotifyGreen else SpotifyWhite,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                var scrubbing by remember { mutableStateOf(false) }
                var scrubPosition by remember { mutableStateOf(0f) }
                val sliderValue = if (scrubbing) scrubPosition else state.progress
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        scrubbing = true
                        scrubPosition = it
                    },
                    onValueChangeFinished = {
                        if (state.durationMs > 0) {
                            onSeek((scrubPosition * state.durationMs).toLong())
                        }
                        scrubbing = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = SpotifyWhite,
                        activeTrackColor = SpotifyWhite,
                        inactiveTrackColor = SpotifyWhite.copy(alpha = 0.25f),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDuration(
                            if (scrubbing) (scrubPosition * state.durationMs).toLong() else state.positionMs,
                        ),
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = formatDuration(state.durationMs),
                        color = SpotifyTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    enabled = state.queueSize > 1,
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffleEnabled) SpotifyGreen else SpotifyTextMuted,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(
                    onClick = onPrevious,
                    enabled = state.currentSong != null,
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = if (state.currentSong != null) SpotifyWhite else SpotifyTextMuted,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SpotifyWhite)
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = SpotifyBackground,
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(
                    onClick = onNext,
                    enabled = state.currentSong != null,
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = if (state.currentSong != null) SpotifyWhite else SpotifyTextMuted,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Spacer(modifier = Modifier.size(24.dp))
            }
            if (state.queueSize > 0) {
                Text(
                    text = "${state.queueSize} songs in queue",
                    color = SpotifyTextMuted,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
