package hu.bme.aut.onlab.valivalter.chessanalyzer.chessboarddetector

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.*

class ChessboardDetector {
    companion object {
        fun findBoard(originalBitmap: Bitmap): Bitmap? {
            val originalMat = Mat()
            Utils.bitmapToMat(originalBitmap, originalMat)
            try {
                val blackAndWhite = toBlackAndWhite(originalMat)
                val blur = blur(blackAndWhite.first)
                val cannyEdges = canny(blur.first)
                val houghLines = getChessboardLines(cannyEdges.first)

                val horizontalLineAnglesMode = houghLines.first.first.groupingBy {
                    (it.second*100).toInt()
                }.eachCount().maxByOrNull { it.value }!!.key/100.0

                val intersections = getIntersections(houghLines.first.first, houghLines.first.second, houghLines.third)
                val tileCorners = getTileCorners(intersections.first, houghLines.third)
                val chessboardCorners = getChessboardCorners(tileCorners.first, houghLines.third, originalBitmap, horizontalLineAnglesMode)
                val chessboard = warpAndCropImage(chessboardCorners.first, originalMat)
                return chessboard
            }
            catch (throwable: Throwable) {
                return null
            }
        }

        private fun matToBitmap(mat: Mat): Bitmap {
            val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, bitmap)
            return bitmap
        }

        private fun toBlackAndWhite(coloredMat: Mat): Pair<Mat, Bitmap> {
            val blackAndWhiteMat = Mat(coloredMat.size(), CvType.CV_8UC1)
            Imgproc.cvtColor(coloredMat, blackAndWhiteMat, Imgproc.COLOR_RGB2GRAY, 4)
            val blackAndWhiteBitmap = matToBitmap(blackAndWhiteMat)
            return Pair(blackAndWhiteMat, blackAndWhiteBitmap)
        }

        private fun blur(mat: Mat): Pair<Mat, Bitmap> {
            val blurredMat = Mat(mat.size(), CvType.CV_8UC1)
            Imgproc.blur(mat, blurredMat, Size(15.0, 15.0))
            val blurredBitmap = matToBitmap(blurredMat)
            return Pair(blurredMat, blurredBitmap)
        }

        private fun canny(mat: Mat): Pair<Mat, Bitmap> {
            val cannyEdgesMat = Mat(mat.size(), CvType.CV_8UC1)
            Imgproc.Canny(mat, cannyEdgesMat, 20.0, 50.0)
            val cannyEdgesBitmap = matToBitmap(cannyEdgesMat)
            return Pair(cannyEdgesMat, cannyEdgesBitmap)
        }

        private fun getChessboardLines(mat: Mat): Triple<Pair<MutableList<Pair<Double, Double>>, MutableList<Pair<Double, Double>>>, Bitmap, Mat> {
            // Színessé kell konvertálni, mert a detektált vonalakat ábrázoló bitmaphez lesz rá szükség
            val houghLinesMat = Mat()
            Imgproc.cvtColor(mat, houghLinesMat, Imgproc.COLOR_GRAY2BGR)

            val lines = Mat()
            Imgproc.HoughLines(mat, lines, 1.0, 3.14/180, 150)
            var (horizontalLines, verticalLines) = getHorizontalAndVerticalLines(lines)
            horizontalLines = removeWrongLines(horizontalLines)
            verticalLines = removeWrongLines(verticalLines)

            for (line in horizontalLines) {
                val rho = line.first
                val theta = line.second
                val a = cos(theta)
                val b = sin(theta)
                val x0 = a * rho
                val y0 = b * rho
                val pt1 = Point(Math.round(x0 + 10000 * -b).toDouble(), Math.round(y0 + 10000 * a).toDouble())
                val pt2 = Point(Math.round(x0 - 10000 * -b).toDouble(), Math.round(y0 - 10000 * a).toDouble())
                Imgproc.line(houghLinesMat, pt1, pt2, Scalar(0.0, 0.0, 255.0), 3, Imgproc.LINE_AA, 0)
            }
            for (line in verticalLines) {
                val rho = line.first
                val theta = line.second
                val a = cos(theta)
                val b = sin(theta)
                val x0 = a * rho
                val y0 = b * rho
                val pt1 = Point(Math.round(x0 + 10000 * -b).toDouble(), Math.round(y0 + 10000 * a).toDouble())
                val pt2 = Point(Math.round(x0 - 10000 * -b).toDouble(), Math.round(y0 - 10000 * a).toDouble())
                Imgproc.line(houghLinesMat, pt1, pt2, Scalar(0.0, 0.0, 255.0), 3, Imgproc.LINE_AA, 0)
            }

            val houghLinesBitmap = matToBitmap(houghLinesMat)
            return Triple(Pair(horizontalLines, verticalLines), houghLinesBitmap, houghLinesMat)
        }

        private fun getHorizontalAndVerticalLines(lines: Mat): Pair<MutableList<Pair<Double, Double>>, MutableList<Pair<Double, Double>>> {
            val verticalLines: MutableList<Pair<Double, Double>> = mutableListOf()
            val horizontalLines: MutableList<Pair<Double, Double>> = mutableListOf()

            for (x in 0 until lines.rows()) {
                val distance = lines[x, 0][0]
                val angle = lines[x, 0][1]
                if (angle < 0.25*Math.PI || angle > 0.75*Math.PI) {
                    verticalLines.add(Pair(distance, angle))
                }
                else {
                    horizontalLines.add(Pair(distance, angle))
                }
            }

            return Pair(horizontalLines, verticalLines)
        }

        private fun removeWrongLines(lines: MutableList<Pair<Double, Double>>): MutableList<Pair<Double, Double>> {
            val anglesMode = lines.groupingBy {
                (it.second*100).toInt()
            }.eachCount().maxByOrNull { it.value }!!.key/100.0

            // 0 radián == pí radián miatti szükséges elágazások
            if (anglesMode > 3.09) {
                val diff = anglesMode - 3.09
                lines.removeIf {
                    abs(it.second - anglesMode) > 0.05 && it.second > diff
                }
            }
            else if (anglesMode < 0.05) {
                val diff = 0.05 - anglesMode
                lines.removeIf {
                    abs(it.second - anglesMode) > 0.05 && it.second < 3.14 - diff
                }
            }
            else {
                lines.removeIf {
                    abs(it.second - anglesMode) > 0.05
                }
            }

            lines.sortBy {
                it.first
            }

            var remainingOutliers = true
            while (remainingOutliers) {
                remainingOutliers = false

                val lineClusterCenters = getLineClusters(lines)

                val Line4 = lineClusterCenters[3]
                val Line5 = lineClusterCenters[4]
                val Line6 = lineClusterCenters[5]
                val tileLength = ((Line5 - Line4) + (Line6 - Line5))/2

                // túl távoli véletlen vonalak, vagy a tábla szélének eltávolítására, ha sötét a háttér
                if ((lineClusterCenters[lineClusterCenters.size-1] - lineClusterCenters[lineClusterCenters.size-2]) > 1.2*tileLength) {
                    remainingOutliers = lines.removeIf {
                        abs(it.first) > lineClusterCenters[lineClusterCenters.size-1] - 0.1*tileLength
                    }
                }
            }

            remainingOutliers = true
            while (remainingOutliers) {
                remainingOutliers = false

                val lineClusterCenters = getLineClusters(lines)

                val Line4 = lineClusterCenters[3]
                val Line5 = lineClusterCenters[4]
                val Line6 = lineClusterCenters[5]
                val tileLength = ((Line5 - Line4) + (Line6 - Line5))/2

                // túl távoli véletlen vonalak, vagy a tábla szélének eltávolítására, ha sötét a háttér
                if ((lineClusterCenters[1] - lineClusterCenters[0]) > 1.2*tileLength) {
                    remainingOutliers = lines.removeIf {
                        abs(it.first) < lineClusterCenters[0] + 0.1*tileLength
                    }
                }
            }

            return lines
        }

        private fun getLineClusters(lines: MutableList<Pair<Double, Double>>): MutableList<Double> {
            val lineDistances = mutableListOf<Point>()
            for (line in lines) {
                lineDistances.add(Point(abs(line.first), 0.0))
            }

            val lineDistancesMat = org.opencv.utils.Converters.vector_Point_to_Mat(lineDistances, CvType.CV_32F)
            val lineClusterCentersMat = Mat()
            Core.kmeans(lineDistancesMat, 9, Mat(), TermCriteria(), 5, Core.KMEANS_PP_CENTERS, lineClusterCentersMat)

            val lineClusterCenters = mutableListOf<Double>()
            for (i in 0 until lineClusterCentersMat.rows()) {
                lineClusterCenters.add(lineClusterCentersMat[i, 0][0])
            }

            lineClusterCenters.sortBy { it }
            return lineClusterCenters
        }


        private fun getIntersections(horizontalLines: MutableList<Pair<Double, Double>>,
                                     verticalLines: MutableList<Pair<Double, Double>>,
                                     linesImageMat: Mat): Pair<MutableList<Point>, Bitmap> {
            val intersectionsImageMat = linesImageMat.clone()

            val points = mutableListOf<Point>()
            for (i in 0 until horizontalLines.size) {
                for (j in 0 until verticalLines.size) {
                    val d1 = horizontalLines[i].first
                    val a1 = horizontalLines[i].second
                    val d2 = verticalLines[j].first
                    val a2 = verticalLines[j].second

                    // lineáris egyenletrendszer megoldása
                    val y = (d2 - (d1 * cos(a2)) / cos(a1)) / (sin(a2) - (sin(a1) * cos(a2)) / cos(a1))
                    val x = d1 / cos(a1) - y * (sin(a1) / cos(a1))
                    val point = Point(x, y)
                    points.add(point)
                    Imgproc.line(intersectionsImageMat, point, point, Scalar(255.0, 0.0, 0.0), 5, Imgproc.LINE_AA, 0)
                }
            }
            val intersectionsBitmap = matToBitmap(intersectionsImageMat)
            return Pair(points, intersectionsBitmap)
        }

        private fun getTileCorners(points: MutableList<Point>, linesImageMat: Mat): Pair<Mat, Bitmap> {
            val clustersImageMat = linesImageMat.clone()

            val pointsMat = org.opencv.utils.Converters.vector_Point_to_Mat(points, CvType.CV_32F)
            val centers = Mat()
            Core.kmeans(pointsMat, 9*9, Mat(), TermCriteria(), 5, Core.KMEANS_PP_CENTERS, centers)
            for (x in 0 until centers.rows()) {
                val xCoord = centers[x, 0][0]
                val yCoord = centers[x, 1][0]
                val point = Point(xCoord, yCoord)
                Imgproc.line(clustersImageMat, point, point, Scalar(255.0, 0.0, 0.0), 20, Imgproc.LINE_AA, 0)
            }
            val clustersBitmap = matToBitmap(clustersImageMat)
            return Pair(centers, clustersBitmap)
        }

        private fun getChessboardCorners(tileCorners: Mat, linesImageMat: Mat, originalBitmap: Bitmap, horizontalLineAnglesMode: Double): Pair<MutableList<Point>, Bitmap> {
            val cornersImageMat = linesImageMat.clone()

            val cornerPoints = mutableListOf<Point>()

            // Ha eléggé ferde a sakktábla a képen
            if (horizontalLineAnglesMode < (7.0/16.0) * Math.PI || horizontalLineAnglesMode > (9.0/16.0) * Math.PI) {
                var minXPoint = Point(tileCorners[0, 0][0], tileCorners[0, 1][0])
                var maxXPoint = Point(tileCorners[0, 0][0], tileCorners[0, 1][0])
                var minYPoint = Point(tileCorners[0, 0][0], tileCorners[0, 1][0])
                var maxYPoint = Point(tileCorners[0, 0][0], tileCorners[0, 1][0])
                for (i in 0 until tileCorners.rows()) {
                    val x = tileCorners[i, 0][0]
                    val y = tileCorners[i, 1][0]
                    if (x < minXPoint.x) { minXPoint = Point(x, y) }
                    if (x > maxXPoint.x) { maxXPoint = Point(x, y) }
                    if (y < minYPoint.y) { minYPoint = Point(x, y) }
                    if (y > maxYPoint.y) { maxYPoint = Point(x, y) }
                }
                cornerPoints.add(minYPoint)
                cornerPoints.add(maxXPoint)
                cornerPoints.add(minXPoint)
                cornerPoints.add(maxYPoint)
            }
            else {
                cornerPoints.add(getClosestPoint(Point(0.0, 0.0), tileCorners))
                cornerPoints.add(getClosestPoint(Point(0.0, originalBitmap.height.toDouble()), tileCorners))
                cornerPoints.add(getClosestPoint(Point(originalBitmap.width.toDouble(), 0.0), tileCorners))
                cornerPoints.add(getClosestPoint(Point(originalBitmap.width.toDouble(), originalBitmap.height.toDouble()), tileCorners))
            }

            val boardCenter = Point((cornerPoints[0].x + cornerPoints[1].x + cornerPoints[2].x + cornerPoints[3].x)/4,
                (cornerPoints[0].y + cornerPoints[1].y + cornerPoints[2].y + cornerPoints[3].y)/4)
            for (i in 0 until 4) {
                for (j in 0 until 4) {
                    val d1 = getDistance(boardCenter, cornerPoints[i])
                    val d2 = getDistance(boardCenter, cornerPoints[j])
                    if ((d1 + d2)/2.0 > 1.1 * d1 ||
                        (d1 + d2)/2.0 < 0.9 * d1 ||
                        (d1 + d2)/2.0 > 1.1 * d2 ||
                        (d1 + d2)/2.0 < 0.9 * d2) {
                        throw Exception("Could not detect chessboard!")
                    }
                }
            }

            cornerPoints.forEach {
                Imgproc.line(cornersImageMat, it, it, Scalar(255.0, 0.0, 0.0), 20, Imgproc.LINE_AA, 0)
            }
            val cornersBitmap = matToBitmap(cornersImageMat)
            return Pair(cornerPoints, cornersBitmap)
        }

        private fun getDistance(p1: Point, p2: Point): Double {
            return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
        }

        private fun getClosestPoint(point: Point, points: Mat): Point {
            var minPoint = Point(points[0, 0][0], points[0, 1][0])
            var minDist = (point.x - minPoint.x).pow(2) + (point.y - minPoint.y).pow(2)
            for (x in 0 until points.rows()) {
                val xCoord = points[x, 0][0]
                val yCoord = points[x, 1][0]
                val dist = (point.x - xCoord).pow(2) + (point.y - yCoord).pow(2)
                if (dist < minDist) {
                    minPoint = Point(xCoord, yCoord)
                    minDist = dist
                }
            }
            return minPoint
        }

        private fun warpAndCropImage(cornerPoints: MutableList<Point>, originalMat: Mat): Bitmap {
            val boardLength = 1000.0
            val goalPoints = listOf(
                Point(0.0, 0.0),
                Point(0.0, boardLength),
                Point(boardLength, 0.0),
                Point(boardLength, boardLength)
            )
            val cornersMat = org.opencv.utils.Converters.vector_Point_to_Mat(cornerPoints, CvType.CV_32F)
            val goalPointsMat = org.opencv.utils.Converters.vector_Point_to_Mat(goalPoints, CvType.CV_32F)
            val transformMatrix = Imgproc.getPerspectiveTransform(cornersMat, goalPointsMat)
            val result = Mat()
            Imgproc.warpPerspective(originalMat, result, transformMatrix, Size(boardLength, boardLength))

            return matToBitmap(result)
        }
    }
}