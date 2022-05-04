package app.actionmobile.cyapass

class LibreStoreJson (var success: Boolean ) {

    val cyabucket: CyaBucket = CyaBucket()
    val message : String = ""

    constructor(success: Boolean, cyabucket: CyaBucket) : this(success){
    }
    constructor(success: Boolean, message: String) : this(success)
}