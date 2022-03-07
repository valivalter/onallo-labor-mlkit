package hu.valivalter.onlab.mlkittest.logic

import com.google.mlkit.vision.label.ImageLabel

class ImageLabelerLogic(val infoScreen : PoseInfoScreen): Logic {
    fun updateLabel(labels : List<ImageLabel>) {

        var mostConfidenceIndex = 0

        for (i in labels.indices) {
            if (labels[i].confidence > labels[mostConfidenceIndex].confidence)
                mostConfidenceIndex = i
        }

        var label = labels[mostConfidenceIndex]
        infoScreen.colorInfo("${label.text}, ${label.confidence}")
    }
}