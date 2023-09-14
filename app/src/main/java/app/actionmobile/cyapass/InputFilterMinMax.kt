package app.actionmobile.cyapass

import android.text.InputFilter
import android.text.Spanned

class InputFilterMinMax(min:Int, max:Int): InputFilter {
    private var min:Int = 1
    private var max:Int = 500

    init{
        this.min = min
        this.max = max
    }

    override fun filter(source:CharSequence, start:Int, end:Int, dest: Spanned, dstart:Int, dend:Int): CharSequence? {
        try
        {
            var input: Int = (dest.subSequence(0, dstart).toString() + source + dest.subSequence(dend, dest.length)).toInt()
            if (isInRange(min, max, input))
                return null
        }
        catch (nfe:NumberFormatException) {}
        return ""
    }

    private fun isInRange(a:Int, b:Int, c:Int):Boolean {
        return if (b > a) c in a..b else c in b..a
    }
}