package app.actionmobile.cyapass

import android.graphics.Point

import java.util.ArrayList
import java.util.HashSet

/**
 * Created by roger.deutsch on 10/19/2016.
 */
object UserPath {
    internal var allPoints: MutableList<Point> = ArrayList()
    var allSegments = HashSet<Segment>()
    private var currentPoint: Point? = null
    var PointValue: Int = 0
    private var previousPostValue: Int = 0

    public fun init(){
        allSegments = HashSet<Segment>()
        currentPoint = null
        PointValue = 0
        previousPostValue = 0
    }
    fun append(currentPoint: Point, postValue: Int) {
        this.currentPoint = currentPoint

        if (allPoints.size >= 1) {
            val currentSegmentCount = allSegments.size
            if (allPoints[allPoints.size - 1].x == currentPoint.x && allPoints[allPoints.size - 1].y == currentPoint.y) {
                // user clicked the same point twice
                return
            }
            allSegments.add(
                Segment(
                    allPoints[allPoints.size - 1],
                    currentPoint, postValue + previousPostValue
                )
            )
        }
        allPoints.add(currentPoint)
        previousPostValue = postValue
    }

    fun CalculateGeometricValue() {
        this.PointValue = 0
        for (s in allSegments) {
            this.PointValue += s.PointValue
        }
    }
}
