package hu.valivalter.onlab.mlkittest.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.RectF
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.valivalter.onlab.mlkittest.logic.DetectionTrackingLogic
import hu.valivalter.onlab.mlkittest.logic.PoseImageAnalyzer
import hu.valivalter.onlab.mlkittest.logic.PoseInfoScreen

class MainViewModel(application : Application) : AndroidViewModel(application), PoseInfoScreen {

    private val _infoText : MutableLiveData<String> = MutableLiveData()
    var infoText : LiveData<String> = _infoText

    @SuppressLint("StaticFieldLeak")
    lateinit var rectOverlay: RectOverlay

    //val poseLogic = PoseLogic(this)


    val trackingLogic = DetectionTrackingLogic(this).apply {
        this.detectedListener = object : DetectionTrackingLogic.DetectorListener {
            override fun onObjectsDetected(bounds: List<RectF>, trackingIds: List<Int>, labels: MutableList<String>) {
                rectOverlay.post { rectOverlay.drawBounds(bounds, trackingIds, labels) }
            }
        }
    }

    //val imageLabelerLogic = ImageLabelerLogic(this)

    val analiser = PoseImageAnalyzer(trackingLogic, this)


    override fun colorInfo(text: String) {
        _infoText.value = text
    }

    override fun getContext(): Context = getApplication()


    lateinit var tts: TextToSpeech

    override fun speak(word: String) {
        tts!!.speak(word, TextToSpeech.QUEUE_FLUSH, null,"")
    }
}