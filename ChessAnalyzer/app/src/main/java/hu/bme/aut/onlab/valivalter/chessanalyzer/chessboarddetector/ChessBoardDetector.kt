package hu.bme.aut.onlab.valivalter.chessanalyzer.chessboarddetector

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin


fun findBoard(originalBitmap: Bitmap): Bitmap? {
    val originalMat = Mat()
    Utils.bitmapToMat(originalBitmap, originalMat)
    try {
        val blackAndWhite = toBlackAndWhite(originalMat)
        val blur = blur(blackAndWhite.first)
        val cannyEdges = canny(blur.first)
        val houghLines = houghLines(cannyEdges.first)
        val intersections = getIntersections(houghLines.first, houghLines.third)
        val clusterCenters = getClusterCenters(intersections.first, houghLines.third)
        val chessboardCorners = getChessboardCorners(clusterCenters.first, houghLines.third, originalBitmap)
        val chessboard = warpAndCropImage(chessboardCorners.first, originalMat)
        return chessboard
    }
    catch (throwable: Throwable) {
        return null
    }
}

fun matToBitmap(mat: Mat): Bitmap {
    val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, bitmap)
    return bitmap
}

fun toBlackAndWhite(coloredMat: Mat): Pair<Mat, Bitmap> {
    val blackAndWhiteMat = Mat(coloredMat.size(), CvType.CV_8UC1)
    Imgproc.cvtColor(coloredMat, blackAndWhiteMat, Imgproc.COLOR_RGB2GRAY, 4)
    val blackAndWhiteBitmap = matToBitmap(blackAndWhiteMat)
    return Pair(blackAndWhiteMat, blackAndWhiteBitmap)
}

fun blur(mat: Mat): Pair<Mat, Bitmap> {
    val blurredMat = Mat(mat.size(), CvType.CV_8UC1)
    Imgproc.blur(mat, blurredMat, Size(10.0, 10.0))
    val blurredBitmap = matToBitmap(blurredMat)
    return Pair(blurredMat, blurredBitmap)
}

fun canny(mat: Mat): Pair<Mat, Bitmap> {
    val cannyEdgesMat = Mat(mat.size(), CvType.CV_8UC1)
    Imgproc.Canny(mat, cannyEdgesMat, 50.0, 100.0)
    val cannyEdgesBitmap = matToBitmap(cannyEdgesMat)
    return Pair(cannyEdgesMat, cannyEdgesBitmap)
}

fun houghLines(mat: Mat): Triple<Mat, Bitmap, Mat> {
    // this will be used for the bitmap, it has to be converted to colored mat
    val houghLinesMat = Mat()
    Imgproc.cvtColor(mat, houghLinesMat, Imgproc.COLOR_GRAY2BGR)

    val lines = Mat()
    Imgproc.HoughLines(mat, lines, 1.0, 3.14/180, 150)
    for (x in 0 until lines.rows()) {
        val rho = lines[x, 0][0]
        val theta = lines[x, 0][1]
        val a = cos(theta)
        val b = sin(theta)
        val x0 = a * rho
        val y0 = b * rho
        val pt1 = Point(Math.round(x0 + 10000 * -b).toDouble(), Math.round(y0 + 10000 * a).toDouble())
        val pt2 = Point(Math.round(x0 - 10000 * -b).toDouble(), Math.round(y0 - 10000 * a).toDouble())
        Imgproc.line(houghLinesMat, pt1, pt2, Scalar(0.0, 0.0, 255.0), 3, Imgproc.LINE_AA, 0)
        //val l: DoubleArray = lines.get(x, 0)
        //Imgproc.line(edgesmegminden, Point(l[0], l[1]), Point(l[2], l[3]), Scalar(0.0, 0.0, 255.0), 3, Imgproc.LINE_AA, 0)
    }
    val houghLinesBitmap = matToBitmap(houghLinesMat)
    return Triple(lines, houghLinesBitmap, houghLinesMat)
}

fun getIntersections(lines: Mat, linesImageMat: Mat): Pair<MutableList<Point>, Bitmap> {
    val intersectionsImageMat = linesImageMat.clone()

    val horizontalAndVerticalLines = getHorizontalAndVerticalLines(lines)
    val horizontalLines = horizontalAndVerticalLines.first
    val verticalLines = horizontalAndVerticalLines.second

    val points = mutableListOf<Point>()
    for (i in 0 until horizontalLines.size) {
        for (j in 0 until verticalLines.size) {
            val d1 = horizontalLines[i].first
            val a1 = horizontalLines[i].second
            val d2 = verticalLines[j].first
            val a2 = verticalLines[j].second

            // lineáris egyenletrendszer, papíron ez jött ki
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

fun getHorizontalAndVerticalLines(lines: Mat): Pair<MutableList<Pair<Double, Double>>, MutableList<Pair<Double, Double>>> {
    val verticalLines: MutableList<Pair<Double, Double>> = mutableListOf()
    val horizontalLines: MutableList<Pair<Double, Double>> = mutableListOf()
    for (x in 0 until lines.rows()) {
        val distance = lines[x, 0][0]
        val angle = lines[x, 0][1]
        if (angle < 0.25*3.14 || angle > 0.75*3.14) {
            verticalLines.add(Pair(distance, angle))
        }
        else {
            horizontalLines.add(Pair(distance, angle))
        }
    }
    return Pair(horizontalLines, verticalLines)
}

fun getClusterCenters(points: MutableList<Point>, linesImageMat: Mat): Pair<Mat, Bitmap> {
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

fun getChessboardCorners(centers: Mat, linesImageMat: Mat, originalBitmap: Bitmap): Pair<MutableList<Point>, Bitmap> {
    val cornersImageMat = linesImageMat.clone()

    val cornerPoints = mutableListOf<Point>()
    cornerPoints.add(getClosestPoint(Point(0.0, 0.0), centers))
    cornerPoints.add(getClosestPoint(Point(0.0, originalBitmap.height.toDouble()), centers))
    cornerPoints.add(getClosestPoint(Point(originalBitmap.width.toDouble(), 0.0), centers))
    cornerPoints.add(getClosestPoint(Point(originalBitmap.width.toDouble(), originalBitmap.height.toDouble()), centers))
    cornerPoints.forEach {
        val point = Point(it.x, it.y)
        Imgproc.line(cornersImageMat, point, point, Scalar(255.0, 0.0, 0.0), 20, Imgproc.LINE_AA, 0)
    }
    val cornersBitmap = matToBitmap(cornersImageMat)
    return Pair(cornerPoints, cornersBitmap)
}

fun getClosestPoint(point: Point, points: Mat): Point {
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

fun warpAndCropImage(cornerPoints: MutableList<Point>, originalMat: Mat): Bitmap {
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