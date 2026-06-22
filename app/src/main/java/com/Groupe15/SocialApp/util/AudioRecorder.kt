package com.Groupe15.SocialApp.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AudioRecorder(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }

    fun start(outputFile: File): Boolean {
        try {
            createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(FileOutputStream(outputFile).fd)

                prepare()
                start()

                recorder = this
            }
            return true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            recorder = null
            return false
        }
    }

    fun stop() {
        try {
            recorder?.stop()
            recorder?.reset()
            recorder?.release()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording", e)
        } finally {
            recorder = null
        }
    }
}
