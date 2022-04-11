package hu.valivalter.onlab.mlkittest.logic

import com.google.mlkit.vision.pose.PoseLandmark
import hu.valivalter.onlab.mlkittest.audio.Player

class PoseLogic(val infoScreen : PoseInfoScreen): Logic {

    private val player = Player(infoScreen.getContext())

    var baseinitialized = false

    var noseToRightKnee = 0.0f
    val squatThreshold = 60.0f
    val handsUpThreshold =-40.0f

    var poseLandmarks : List<PoseLandmark> = listOf()

    fun updatePoseLandmarks(newList : List<PoseLandmark>) {
        poseLandmarks = newList

        val nose = poseLandmarks.find{it.landmarkType==PoseLandmark.NOSE}
        val rightKnee = poseLandmarks.find{it.landmarkType==PoseLandmark.RIGHT_KNEE}
        val leftWrist = poseLandmarks.find{it.landmarkType==PoseLandmark.RIGHT_WRIST}
        val rightWrist = poseLandmarks.find{it.landmarkType==PoseLandmark.LEFT_WRIST}

        if(baseinitialized==null && nose != null && rightKnee != null ) {
            baseInitialize(nose, rightKnee)
            infoScreen.colorInfo("Base initialization complete")
        }
        else {
            if(checkSquat()) {
                infoScreen.colorInfo("Jump!")
                player.playJump()
            }
            if(checkRaiseHands()) {
                infoScreen.colorInfo("Hands up!")
                player.playHandsUp()
            }

            if (checkBodyPart(PoseLandmark.NOSE)) {
                infoScreen.speak("Nose")
            }
            if (checkBodyPart(PoseLandmark.LEFT_SHOULDER)) {
                infoScreen.speak("Left shoulder")
            }
            if (checkBodyPart(PoseLandmark.RIGHT_SHOULDER)) {
                infoScreen.speak("Right shoulder")
            }
            if (checkBodyPart(PoseLandmark.LEFT_MOUTH)) {
                infoScreen.speak("Left mouth")
            }
            if (checkBodyPart(PoseLandmark.RIGHT_MOUTH)) {
                infoScreen.speak("Right mouth")
            }

        }
    }

    fun baseInitialize(nose : PoseLandmark, rightKnee : PoseLandmark) {
        baseinitialized = true
        noseToRightKnee = rightKnee.position.y - nose.position.y
    }

    fun checkSquat() : Boolean {
        val nose = poseLandmarks.find{it.landmarkType==PoseLandmark.NOSE}
        val rightKnee = poseLandmarks.find{it.landmarkType==PoseLandmark.RIGHT_KNEE}

        if(nose != null && rightKnee != null) {
            val diff = rightKnee.position.y - nose.position.y
            if (noseToRightKnee - diff > squatThreshold)
                return true
        }
        return false
    }

    fun checkRaiseHands() : Boolean {

        val nose = poseLandmarks.find{it.landmarkType==PoseLandmark.NOSE}
        val leftWrist = poseLandmarks.find{it.landmarkType==PoseLandmark.RIGHT_WRIST}
        val rightWrist = poseLandmarks.find{it.landmarkType==PoseLandmark.LEFT_WRIST}

        if(nose!=null && leftWrist != null && rightWrist != null) {

            val leftdiff = leftWrist.position.y - nose.position.y
            val rightdiff = rightWrist.position.y - nose.position.y

            if ((rightdiff < handsUpThreshold) && (leftdiff < handsUpThreshold))
                return true
        }
        return false
    }



    fun checkBodyPart(bodyPart: Int) : Boolean { // PoseLandmark-os konstans a paraméter
        val leftIndex = poseLandmarks.find{ it.landmarkType == PoseLandmark.LEFT_INDEX }

        val landmark = poseLandmarks.find{ it.landmarkType == bodyPart }

        if(leftIndex != null && landmark != null) {

            val yDiff = Math.abs(landmark.position.y - leftIndex.position.y)
            val xDiff = Math.abs(landmark.position.x - leftIndex.position.x)

            if (xDiff < 30 && yDiff < 30)
                return true
        }
        return false
    }



}