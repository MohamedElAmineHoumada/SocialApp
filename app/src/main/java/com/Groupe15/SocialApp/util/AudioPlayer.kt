package com.Groupe15.SocialApp.util

import android.content.Context
import android.media.MediaPlayer
import androidx.core.net.toUri

class AudioPlayer(
    private val context: Context
) {
    private var player: MediaPlayer? = null

    fun playFile(fileUrl: String, onComplete: () -> Unit = {}) {
        stop()
        player = MediaPlayer.create(context, fileUrl.toUri()).apply {
            setOnCompletionListener {
                onComplete()
                stop()
            }
            start()
        }
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
    }

    fun isPlaying(): Boolean = player?.isPlaying ?: false
}
