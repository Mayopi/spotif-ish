package com.example.spotifish.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spotifish.domain.model.Song
import com.example.spotifish.ui.search.SearchUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = SpotifyWhite,
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Artists, songs, or albums", color = Color.DarkGray) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.DarkGray,
                )
            },
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = SpotifyBackground,
                focusedTextColor = SpotifyBackground,
                unfocusedTextColor = SpotifyBackground,
            ),
        )
        if (state.query.isBlank()) {
            Text(
                text = "Browse all",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = SpotifyWhite,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 140.dp),
            ) {
                items(BROWSE_GENRES) { genre ->
                    GenreTile(genre)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 140.dp),
            ) {
                items(state.results) { song ->
                    SongRow(
                        song = song,
                        onClick = { onPlaySong(song, state.results) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                    )
                }
            }
        }
    }
}

private data class Genre(val label: String, val color: Color)

private val BROWSE_GENRES = listOf(
    Genre("Pop", Color(0xFFE91E63)),
    Genre("Hip-Hop", Color(0xFFFF7043)),
    Genre("Rock", Color(0xFF5C6BC0)),
    Genre("Indie", Color(0xFF26A69A)),
    Genre("Electronic", Color(0xFF7E57C2)),
    Genre("Jazz", Color(0xFF8D6E63)),
    Genre("Classical", Color(0xFF42A5F5)),
    Genre("Chill", Color(0xFF66BB6A)),
    Genre("Workout", Color(0xFFEF5350)),
    Genre("Focus", Color(0xFF26C6DA)),
)

@Composable
private fun GenreTile(genre: Genre) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.8f)
            .clip(RoundedCornerShape(8.dp))
            .background(genre.color)
            .padding(14.dp),
    ) {
        Text(
            text = genre.label,
            color = SpotifyWhite,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
    }
}
