package app.actionmobile.cyapass

import android.util.Base64
import android.util.Base64.*
import android.util.Log

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Created by roger.deutsch on 8/18/2016.
 */
class SiteKey {

    @SerializedName("MaxLength")
    var maxLength: Int = 0
    @SerializedName("HasSpecialChars")
    var isHasSpecialChars: Boolean = false

    @SerializedName("HasUpperCase")
    var isHasUpperCase: Boolean = false

    @SerializedName("Key")
    var key: String
        private set

    constructor(key: String) {
        this.key = key
    }

    constructor(
        key: String, hasSpecialChars: Boolean,
        hasUpperCase: Boolean,
        hasMaxLength: Boolean,
        maxLength: Int
    ) {
        this.key = encode(key.toByteArray(), Base64.DEFAULT).toString()
        this.isHasSpecialChars = hasSpecialChars
        this.isHasUpperCase = hasUpperCase
        this.maxLength = maxLength
    }

    override fun toString(): String {
        return decode(this.key,Base64.DEFAULT).toString()
    }

    companion object {

        fun toJson(sk: List<SiteKey>): String {
            val gson = Gson()
            Log.d("MainActivity", "######################")
            Log.d("MainActivity", gson.toJson(sk))
            return gson.toJson(sk)
        }
    }
}
