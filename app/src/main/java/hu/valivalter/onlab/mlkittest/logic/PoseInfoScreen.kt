package hu.valivalter.onlab.mlkittest.logic

import android.content.Context
import android.graphics.RectF

interface PoseInfoScreen {
    fun colorInfo(text: String)
    fun getContext() : Context

    fun speak(text: String)
}