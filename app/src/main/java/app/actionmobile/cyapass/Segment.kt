package app.actionmobile.cyapass

import android.graphics.Point

/**
 * Created by roger.deutsch on 10/19/2016.
 */
class Segment(internal var Begin: Point, internal var End: Point, internal var PointValue: Int) {


    override fun equals(obj: Any?): Boolean {
        val incoming = `as`(obj, Segment::class.java) ?: return false

        val a =
            this.Begin.x == incoming.Begin.x && this.Begin.y == incoming.Begin.y && this.End.x == incoming.End.x && this.End.y == incoming.End.y

        val b =
            this.Begin.x == incoming.End.x && this.Begin.y == incoming.End.y && this.End.x == incoming.Begin.x && this.End.y == incoming.Begin.y
        return a || b
    }

    private fun `as`(o: Any?, tClass: Class<Segment>): Segment? {
        return if (tClass.isInstance(o)) o as Segment? else null
    }

    override fun hashCode(): Int {
        val flippedHashValueString = String.format(
            "%d%d%d%d",
            this.End.x, this.End.y,
            this.Begin.x, this.Begin.y
        )

        val hashValueString = String.format(
            "%d%d%d%d",
            this.Begin.x, this.Begin.y,
            this.End.x, this.End.y
        )

        return flippedHashValueString.hashCode() + hashValueString.hashCode()
    }
}
