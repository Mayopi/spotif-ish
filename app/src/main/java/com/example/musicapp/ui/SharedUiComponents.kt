package com.example.musicapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicapp.domain.model.Song
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

@Composable
fun ArtworkThumb(
    song: Song,
    modifier: Modifier = Modifier,
) {
    val boxModifier = modifier.clip(RoundedCornerShape(6.dp))
    val gradient = remember(song.id) { gradientForSong(song.id) }
    val initial = remember(song.id, song.title, song.artist) {
        song.title.take(1).ifBlank { song.artist.take(1) }.uppercase()
    }

    Box(
        modifier = boxModifier.background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = SpotifyWhite,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        if (song.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

fun gradientForSong(songId: String): Brush {
    val seed = (songId.hashCode().absoluteValue) % ARTWORK_PALETTES.size
    val pair = ARTWORK_PALETTES[seed]
    return Brush.linearGradient(listOf(pair.first, pair.second))
}

private val ARTWORK_PALETTES: List<Pair<Color, Color>> = listOf(
    Color(0xFF1DB954) to Color(0xFF0F3B22),
    Color(0xFF8E44AD) to Color(0xFF2C1338),
    Color(0xFFE91E63) to Color(0xFF3B0A1E),
    Color(0xFFF39C12) to Color(0xFF3B2208),
    Color(0xFF3498DB) to Color(0xFF0B2540),
    Color(0xFFE74C3C) to Color(0xFF3A0F09),
    Color(0xFF16A085) to Color(0xFF082D24),
    Color(0xFF9B59B6) to Color(0xFF2A1038),
)

@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ArtworkThumb(
                song = song,
                modifier = Modifier.size(52.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SpotifyWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = formatDuration(song.durationMs),
                color = SpotifyTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (song.isFavorite) "Unlike" else "Like",
                    tint = if (song.isFavorite) SpotifyGreen else SpotifyTextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "--:--"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
