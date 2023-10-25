package com.example.playlistmaker.common.data

import com.example.playlistmaker.common.domain.db.PlaylistsDbInteractor
import com.example.playlistmaker.common.domain.db.PlaylistsDbRepository
import com.example.playlistmaker.common.presentation.models.PlaylistInformation
import com.example.playlistmaker.search.domain.models.Track
import kotlinx.coroutines.flow.Flow

class PlaylistsDbInteractorImpl(
    val repository: PlaylistsDbRepository
): PlaylistsDbInteractor {
    override fun giveMeTracksFromPlaylist(playlistId: String): Flow<List<Track>> {
        return repository.getPlaylistTracks(playlistId)
    }

    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        repository.addTrackToPlaylist(playlistId, track)
    }

    override fun giveMeAllPlaylists(): Flow<List<PlaylistInformation>> {
        return repository.allPlaylists()
    }

    override suspend fun addPlaylist(playlist: PlaylistInformation) {
        repository.addPlaylist(playlist)
    }
}