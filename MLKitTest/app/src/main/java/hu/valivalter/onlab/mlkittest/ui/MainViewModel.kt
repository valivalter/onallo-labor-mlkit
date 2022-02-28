package hu.valivalter.onlab.mlkittest.ui

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.valivalter.onlab.mlkittest.logic.PoseImageAnalyzer
import hu.valivalter.onlab.mlkittest.logic.PoseInfoScreen
import hu.valivalter.onlab.mlkittest.logic.PoseLogic

class MainViewModel(application : Application) : AndroidViewModel(application), PoseInfoScreen {

    private val _infoText : MutableLiveData<String> = MutableLiveData()
    var infoText : LiveData<String> = _infoText

    var _squatCount : MutableLiveData<Int> = MutableLiveData(0)
    var squatCount : LiveData<Int> = _squatCount
    var _handsupCount : MutableLiveData<Int> = MutableLiveData(0)
    var handsupCount : LiveData<Int> = _handsupCount


    val poseLogic = PoseLogic(this)
    val analiser = PoseImageAnalyzer(poseLogic, this)


    override fun colorInfo(text: String) {
        _infoText.value = text
    }

    override fun increaseSquat() {
        _squatCount.value =  _squatCount.value!! + 1
    }

    override fun increaseHandsup() {
        _handsupCount.value =  _handsupCount.value!! + 1
    }


    override fun getContext(): Context = getApplication()



    lateinit var tts: TextToSpeech

    override fun speak(word: String) {
        tts!!.speak(word, TextToSpeech.QUEUE_FLUSH, null,"")
    }
}