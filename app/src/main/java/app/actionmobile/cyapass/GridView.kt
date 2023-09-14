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
import java.util.*

/**
 * Created by roger.deutsch on 6/7/2016.
 */
class GridView(private val _context: Context, multiHashCount: Int, multiHashIsOn: Boolean) : View(_context) {

    private var _allPosts: MutableList<Point>? = null
    private var centerPoint: Int = 0
    private val postWidth: Int
    private val leftOffset: Int
    private val highlightOffset: Int
    private val topOffset = 20
    private var xCanvas: Canvas? = null
    private var clearTextPwd : String = ""
    var viewWidth: Int = 0
    var viewHeight: Int = 0
    private var currentPoint: Point? = null
    internal var hitTestIdx: Int = 0
    var density : Float;
    internal var numOfCells = 5
    var cellSize: Int = 0 //125
    var vx: View
    var multiHashIsOn: Boolean
    var multiHashCount: Int

    var ClearTextPwd : String = ""
        get() = this.clearTextPwd

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
        density = resources.displayMetrics.density
        Log.d("MainActivity", "density : $density")
        val densityDPI = resources.displayMetrics.densityDpi
        Log.d("MainActivity", "densityDPI : $densityDPI")
        viewWidth = resources.displayMetrics.widthPixels
        Log.d("MainActivity", "viewWidth : $viewWidth")
        viewHeight = resources.displayMetrics.heightPixels
        Log.d("MainActivity", "viewHeight : $viewHeight")

        vx = this.rootView

        this.multiHashCount = multiHashCount
        this.multiHashIsOn = multiHashIsOn

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
        generateAllPosts()

        setOnTouchListener(OnTouchListener { v, event ->
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
                if (selectNewPoint(currentPoint!!)) {
                    v.invalidate()
                    userPath.CalculateGeometricValue()
                    generatePassword()
                }
            }
            true
        })
    }

    fun clearGrid() {
        if (!isPatternHidden) {
            userPath.init()
        }
        invalidate()
        vx.invalidate()
    }

    private fun drawUserShape(canvas: Canvas) {
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

    private fun drawHighlight(p: Point) {
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

    private fun selectNewPoint(p: Point): Boolean {
        val currentPoint = hitTest(Point(p.x, p.y)) ?: return false
        userPath.append(currentPoint, hitTestIdx + hitTestIdx * (hitTestIdx / 6) * 10)
        userPath.CalculateGeometricValue()

        return true
    }

    fun generatePassword(multiHashIsOn: Boolean, multiHashCount:Int) {
        if (!multiHashIsOn){
            createHash(0)
        }
        else {
            createHash(multiHashCount)
        }
    }

    private fun generatePassword(){
        if (!multiHashIsOn){
            createHash(0)
        }
        else {
            createHash(multiHashCount)
        }
    }

    private fun generateAllPosts() {
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
    private fun createHash(multiHashCount: Int) {
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
        Log.d("MainActivity", "text:   $text")
        this.clearTextPwd = text
        Log.d("MainActivity", "clearTextPwd: ${this.clearTextPwd}")
        var sb = GenHashFromString(text)
        var loopCount = 0;
        Log.d("MainActivity", "sb 1 => ${sb.toString()}")
        while (loopCount < multiHashCount){
            sb = GenHashFromString("${userPath.PointValue}" + sb.toString())
            Log.d("MainActivity", "sb 2 => ${sb.toString()}")
            loopCount++;
        }
            if (currentSiteKey.isHasSpecialChars) {
                // yes, I still get the special chars from what the user typed on the form
                // because I don't store special chars in JSON as a protection
                if (MainActivity.specialChars != null && MainActivity.specialChars !== "") {
                    sb.insert(2, MainActivity.specialChars)
                    Log.d("MainActivity", " ${MainActivity.specialChars.toString().length}")
                    sb = StringBuilder(sb.substring(0, sb.length - MainActivity.specialChars.toString().length))
                }
            }
            if (currentSiteKey.isHasUpperCase) {
                Log.d("MainActivity", "calling addUpperCase()")
                val firstLetterIndex = addUpperCase(sb.toString())
                Log.d("MainActivity", "firstLetterIndex : $firstLetterIndex")
                if (firstLetterIndex >= 0) {
                    // get the string, uppercase it, get the uppercased char at location
                    Log.d("MainActivity", "calling sb.setCharAt()")
                    Log.d("MainActivity", "value : " + sb.toString().uppercase(Locale.getDefault())[firstLetterIndex].toString())
                    sb.setCharAt(firstLetterIndex, sb.toString().uppercase(Locale.getDefault())[firstLetterIndex])
                }
            }
            if (currentSiteKey.maxLength > 0) {
                val temp = StringBuilder()
                temp.insert(0, sb.substring(0, currentSiteKey.maxLength))
                sb = temp
            }
            Log.d("MainActivity", sb.toString())
            MainActivity.SetPassword(sb.toString())

            val clipboard = _context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Copied Text", sb.toString())
            clipboard.setPrimaryClip(clip)



    }
    private fun GenHashFromString(text: String) : StringBuilder{
        var sb = StringBuilder()
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(text.toByteArray(Charset.forName("UTF-8")))
            for (b in hash) {
                sb.append(String.format("%02x", b))
            }
        } catch (nsa: NoSuchAlgorithmException) {}
        return sb;
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

    private fun drawGridLines() {
        val paint = Paint()
        paint.strokeWidth = 1.2f * density;
        paint.color = Color.LTGRAY;
        Log.d("MainActivity", "paint.strokeWidth : " + paint.strokeWidth.toString())
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

    private fun drawPosts() {

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

        drawGridLines()
        drawPosts()

        if (!isPatternHidden) {
            drawUserShape(canvas)
            if (userPath.allPoints.size > 0) {
                drawHighlight(userPath.allPoints[0])
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        this.xCanvas = canvas
        super.dispatchDraw(canvas)

        drawGridLines()
        drawPosts()
        if (!isPatternHidden) {
            drawUserShape(canvas)
            if (userPath.allPoints.size > 0) {
                drawHighlight(userPath.allPoints[0])
            }
        }
    }

    private fun hitTest(p: Point): Point? {
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
