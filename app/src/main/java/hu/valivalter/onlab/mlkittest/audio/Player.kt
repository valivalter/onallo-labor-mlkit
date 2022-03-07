package hu.valivalter.onlab.mlkittest.audio

import android.content.Context
import android.media.MediaPlayer
import hu.valivalter.onlab.mlkittest.R


class Player(context: Context) {
    private lateinit var jumpPlayer: MediaPlayer;
    private lateinit var handsPlayer: MediaPlayer;

    init {
        jumpPlayer = MediaPlayer.create(context, R.raw.hopp)
        handsPlayer = MediaPlayer.create(context, R.raw.kezeketfel)
    }

    fun playJump()
    {
        jumpPlayer.start()
    }

    fun playHandsUp()
    {
        handsPlayer.start()
    }
}