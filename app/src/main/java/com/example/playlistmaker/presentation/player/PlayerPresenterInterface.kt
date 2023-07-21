package com.example.playlistmaker.presentation.player

interface PlayerPresenterInterface {
    var view: PlayerActivityInterface?

    fun bindScreen()

    fun setProgressTime()

    fun changePlayButton()

    fun resetPlayer()
}