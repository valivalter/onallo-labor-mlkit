package hu.valivalter.onlab.mlkittest.logic

import com.google.mlkit.vision.label.ImageLabel

class ImageLabelerLogic(val infoScreen : PoseInfoScreen): Logic {
    fun updateLabel(labels : List<ImageLabel>) {
        if (labels.isNotEmpty()) {
            //var mostConfidenceIndex = 0

            //for (i in labels.indices) {
            //    if (labels[i].confidence > labels[mostConfidenceIndex].confidence)
            //        mostConfidenceIndex = i
            //}


            var screenText = ""
            //var label = labels[mostConfidenceIndex]
            for (i in labels.indices) {
                screenText += "${labels[i].text}, ${labels[i].confidence}\n"
            }
            infoScreen.colorInfo(screenText.dropLast(1))

            /*infoScreen.colorInfo("${labels[0].text}, ${labels[0].confidence}\n" +
                    "${labels[1].text}, ${labels[1].confidence}\n" +
                    "${labels[2].text}, ${labels[2].confidence}\n" +
                    "${labels[3].text}, ${labels[3].confidence}\n" +
                    "${labels[4].text}, ${labels[4].confidence}\n")*/
        }
    }
}