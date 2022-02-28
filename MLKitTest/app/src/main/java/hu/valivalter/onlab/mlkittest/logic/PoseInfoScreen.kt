package hu.valivalter.onlab.mlkittest.logic

import android.content.Context

interface PoseInfoScreen {
    fun colorInfo(text: String)
    fun increaseSquat()
    fun increaseHandsup()
    fun getContext() : Context

    fun speak(text: String)
}