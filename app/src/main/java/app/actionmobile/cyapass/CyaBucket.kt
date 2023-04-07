package app.actionmobile.cyapass

class CyaBucket (var id : Int, var data : String,
                 var hmac : String, var iv : String, var created : String,
                 var updated : String, var active : Boolean) {
        constructor() : this(0, "", "","","","",false)

}
