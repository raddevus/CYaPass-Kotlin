package app.actionmobile.cyapass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.Log
import android.view.MotionEvent
import android.view.View

import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.ArrayList

/**
 * Created by roger.deutsch on 6/7/2016.
 */
class GridView(private val _context: Context) : View(_context) {

    private var _allPosts: MutableList<Point>? = null
    private var centerPoint: Int = 0
    private val postWidth: Int
    private val leftOffset: Int
    private val highlightOffset: Int
    private val topOffset = 20
    private var xCanvas: Canvas? = null
    var viewWidth: Int = 0
    var viewHeight: Int = 0
    private var currentPoint: Point? = null
    internal var hitTestIdx: Int = 0

    internal var numOfCells = 5
    var cellSize: Int = 0 //125
    var vx: View

    public var userPath = UserPath

    //if (userPath == null || userPath.allSegments == null){return false;}
    val isLineSegmentComplete: Boolean
        get() {
            Log.d("MainActivity", "size ; " + userPath.allSegments.size.toString())
            Log.d("MainActivity", "size ; " + this.userPath.allSegments.size.toString())
            Log.d("MainActivity", "size ; " + userPath.allPoints.size.toString())
            return java.lang.Boolean.valueOf(userPath.allSegments.size > 0)
        }

    init {

        /*        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } */
        val density = resources.displayMetrics.density
        Log.d("MainActivity", "density : $density")
        val densityDPI = resources.displayMetrics.densityDpi
        Log.d("MainActivity", "densityDPI : $densityDPI")
        viewWidth = resources.displayMetrics.widthPixels
        Log.d("MainActivity", "viewWidth : $viewWidth")
        viewHeight = resources.displayMetrics.heightPixels
        Log.d("MainActivity", "viewHeight : $viewHeight")

        vx = this.rootView

        Log.d("MainActivity", "id: " + vx.id.toString())
        //postWidth = ((viewWidth / 2) / 6) /5;
        postWidth = viewWidth / 58
        highlightOffset = postWidth + 10
        centerPoint = viewWidth / 7
        cellSize = centerPoint
        leftOffset = viewWidth - (numOfCells + 1) * cellSize //(viewWidth / densityDPI) * 6;

        Log.d("MainActivity", "postWidth : $postWidth")
        //leftOffset = (int)(viewWidth / .9) / 4;

        //leftOffset = (int)(viewWidth / .9) / 4;
        Log.d("MainActivity", "leftOffset : $leftOffset")
        GenerateAllPosts()

        this.setOnTouchListener(OnTouchListener { v, event ->
            if (isPatternHidden) {
                return@OnTouchListener true
            }
            if (event.action == MotionEvent.ACTION_DOWN) {
                val touchX = event.x.toInt()
                val touchY = event.y.toInt()
                val output = "Touch coordinates : " +
                        touchX.toString() + "x" + touchY.toString()
                //Toast.makeText(v.getContext(), output, Toast.LENGTH_SHORT).show();
                currentPoint = Point(touchX, touchY)
                if (SelectNewPoint(currentPoint!!)) {
                    v.invalidate()
                    userPath.CalculateGeometricValue()
                    GeneratePassword()
                }
            }
            true
        })
    }

    fun ClearGrid() {
        if (!isPatternHidden) {
            userPath.init()
        }
        invalidate()
        vx.invalidate()
    }

    private fun DrawUserShape(canvas: Canvas) {
        val paint = Paint()
        paint.color = Color.BLUE
        paint.strokeWidth = 8f
        paint.style = Paint.Style.STROKE

        for (s in userPath.allSegments) {
            canvas.drawCircle(
                s.Begin.x.toFloat(),
                s.Begin.y.toFloat(),
                highlightOffset.toFloat(), paint
            )
            canvas.drawLine(
                s.Begin.x.toFloat(), s.Begin.y.toFloat(),
                s.End.x.toFloat(),
                s.End.y.toFloat(), paint
            )
            //            Log.d("MainActivity", "DONE drawing line...");
        }

    }

    private fun DrawHighlight(p: Point) {
        Log.d("MainActivity", "DrawHighlight()...")
        Log.d("MainActivity", p.toString())
        val paint = Paint()
        if (userPath.allPoints.size == 1) {
            paint.color = Color.CYAN
        } else {
            paint.color = Color.BLUE
        }
        paint.strokeWidth = 8f
        paint.style = Paint.Style.STROKE

        xCanvas!!.drawCircle(
            p.x.toFloat(),
            p.y.toFloat(),
            (postWidth + 10).toFloat(), paint
        )

    }

    private fun SelectNewPoint(p: Point): Boolean {
        val currentPoint = HitTest(Point(p.x, p.y)) ?: return false
        userPath.append(currentPoint, hitTestIdx + hitTestIdx * (hitTestIdx / 6) * 10)
        userPath.CalculateGeometricValue()

        return true
    }

    fun GeneratePassword() {
        CreateHash()
    }

    private fun GenerateAllPosts() {
        _allPosts = ArrayList()
        // NOTE: removed the -(postWid/2) because drawLine works via centerpoint instead of offset like C#
        for (x in 0..5) {
            for (y in 0..5) {
                _allPosts!!.add(Point(leftOffset + centerPoint * x, topOffset + centerPoint * y))
                Log.d("Extra", "Point.x = " + (leftOffset + centerPoint * x).toString())
                Log.d("Extra", "Point.y = " + (topOffset + centerPoint * y).toString())
            }
        }
    }

    //@TargetApi(19)
    private fun CreateHash() {
        //String site = MainActivity.siteKey.getText().toString();  //"amazon";
        if (MainActivity.currentSiteKey.key == "") {
            MainActivity.SetPassword("")
            return
        }
        if (!isLineSegmentComplete) {
            MainActivity.SetPassword("")
            return
        }
        val currentSiteKey = MainActivity.currentSiteKey
        Log.d("MainActivity", "site: " + currentSiteKey.toString())
        val text = "${userPath.PointValue}${currentSiteKey}"
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(text.toByteArray(Charset.forName("UTF-8")))
            var sb = StringBuilder()
            for (b in hash) {
                sb.append(String.format("%02x", b))
            }
            if (currentSiteKey!!.isHasSpecialChars) {
                // yes, I still get the special chars from what the user typed on the form
                // because I don't store special chars in JSON as a protection
                if (MainActivity.specialChars != null && MainActivity.specialChars !== "") {
                    sb.insert(2, MainActivity.specialChars)
                }
            }
            if (currentSiteKey!!.isHasUpperCase) {
                Log.d("MainActivity", "calling addUpperCase()")
                val firstLetterIndex = addUpperCase(sb.toString())
                Log.d("MainActivity", "firstLetterIndex : $firstLetterIndex")
                if (firstLetterIndex >= 0) {
                    // get the string, uppercase it, get the uppercased char at location
                    Log.d("MainActivity", "calling sb.setCharAt()")
                    Log.d("MainActivity", "value : " + sb.toString().toUpperCase()[firstLetterIndex].toString())
                    sb.setCharAt(firstLetterIndex, sb.toString().toUpperCase()[firstLetterIndex])
                }
            }
            if (currentSiteKey?.maxLength > 0) {
                val temp = StringBuilder()
                temp.insert(0, sb.substring(0, currentSiteKey!!.maxLength))
                sb = temp
            }
            Log.d("MainActivity", sb.toString())
            MainActivity.SetPassword(sb.toString())

            val clipboard = _context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Copied Text", sb.toString())
            clipboard.setPrimaryClip(clip)

        } catch (nsa: NoSuchAlgorithmException) {

        }

    }

    private fun addUpperCase(sb: String): Int {
        val entireString = CharArray(sb.length - 1)
        var indexCounter = 0
        sb.toCharArray(entireString, 0, 0, sb.length - 1)
        for (c in entireString) {
            if (Character.isLetter(c)) {
                return indexCounter
            }
            indexCounter++
        }
        return -1
    }

    private fun DrawGridLines() {
        val paint = Paint()

        for (y in 0..numOfCells) {
            xCanvas!!.drawLine(
                (0 + leftOffset).toFloat(), (y * cellSize + topOffset).toFloat(),
                (numOfCells * cellSize + leftOffset).toFloat(),
                (y * cellSize + topOffset).toFloat(), paint
            )
        }

        for (x in 0..numOfCells) {
            xCanvas!!.drawLine(
                (x * cellSize + leftOffset).toFloat(), (0 + topOffset).toFloat(),
                (x * cellSize + leftOffset).toFloat(), (numOfCells * cellSize + topOffset).toFloat(),
                paint
            )
        }

    }

    private fun DrawPosts() {

        val paint = Paint()
        // Use Color.parseColor to define HTML colors
        paint.color = Color.parseColor("#CD5C5C")

        for (Pt in _allPosts!!) {
            xCanvas!!.drawCircle(Pt.x.toFloat(), Pt.y.toFloat(), postWidth.toFloat(), paint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        this.xCanvas = canvas
        super.onDraw(canvas)

        DrawPosts()
        DrawGridLines()
        if (!isPatternHidden) {
            DrawUserShape(canvas)
            if (userPath.allPoints.size > 0) {
                DrawHighlight(userPath.allPoints[0])
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        this.xCanvas = canvas
        super.dispatchDraw(canvas)

        DrawPosts()
        DrawGridLines()
        if (!isPatternHidden) {
            DrawUserShape(canvas)
            if (userPath.allPoints.size > 0) {
                DrawHighlight(userPath.allPoints[0])
            }
        }
    }

    private fun HitTest(p: Point): Point? {
        var loopcount = 0
        hitTestIdx = 0
        for (Pt in _allPosts!!) {
            if (p.x >= Pt.x - postWidth * 2 && p.x <= Pt.x + postWidth * 2) {
                if (p.y >= Pt.y - postWidth * 2 && p.y <= Pt.y + postWidth * 2) {
                    //String output = String.format("it's a hit: %d %d",p.x,p.y);
                    //Toast.makeText(this.getContext(), output, Toast.LENGTH_SHORT).show();
                    hitTestIdx = loopcount
                    return Pt
                }
            }
            loopcount++
        }

        return null
    }

    fun isPatternHidden(): Boolean {
        return isPatternHidden
    }

    fun setPatternHidden(patternHidden: Boolean) {
        isPatternHidden = patternHidden
    }

    companion object {

        var userPath = UserPath
        private var isPatternHidden: Boolean = false
    }
}
