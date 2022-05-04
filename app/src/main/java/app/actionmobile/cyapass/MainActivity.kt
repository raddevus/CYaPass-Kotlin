package app.actionmobile.cyapass

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.newlibre.aescrypt.Crypton

class MainActivity : AppCompatActivity() {

     private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    /**
     * The [ViewPager] that will host the section contents.
     */
    private var mViewPager: ViewPager? = null
    internal var pairedDevices: Set<BluetoothDevice>? = null
    private var btAdapter: BluetoothAdapter? = null
    internal var ct: ConnectThread? = null
    private var tabLayout: TabLayout? = null

    private val layout1: LinearLayout? = null

    private fun clearClipboard() {
        val clipboard = appContext!!.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        //android.content.ClipData clip = android.content.ClipData.newPlainText("", "");
        val clip = android.content.ClipData.newPlainText(null, null)
        clipboard.setPrimaryClip(clip)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MainActivity.appContext = applicationContext
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container) as ViewPager
        mViewPager!!.adapter = mSectionsPagerAdapter

        tabLayout = findViewById(R.id.tabs) as TabLayout
        tabLayout?.setupWithViewPager(mViewPager)

        val sendFab = findViewById<FloatingActionButton>(R.id.sendFab)
        sendFab.setOnClickListener(View.OnClickListener {
            if (MainActivity.btCurrentDeviceName === "") {
                return@OnClickListener
            }
            sendPasswordViaBT()
            if (isSendCtrlAltDel) {
                ct!!.writeCtrlAltDel()
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    e.message?.let { it1 -> Log.d("MainActivity", it1) }
                }

            }
            writeData()
        })
    }

    /** Called before the activity is destroyed  */
    public override fun onDestroy() {

        super.onDestroy()
    }

    private fun sendPasswordViaBT() {

        if (btAdapter == null) {
            btAdapter = BluetoothAdapter.getDefaultAdapter()
        }
        if (btAdapter != null) {
            if (!btAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        } else {
            Log.d("MainActivity", "no bt adapter available")
            return  // cannot get btadapter
        }

        if (pairedDevices == null) {
            pairedDevices = btAdapter!!.bondedDevices
        }
        if (pairedDevices!!.size > 0) {

            for (btItem in pairedDevices!!) {
                if (btItem != null) {
                    val name = btItem.name
                    if (name == MainActivity.btCurrentDeviceName) {
                        val uuid = btItem.uuids[0].uuid
                        Log.d("MainActivity", uuid.toString())
                        if (ct == null) {
                            ct = ConnectThread(btItem, uuid, null)
                        }
                        ct!!.run(btAdapter!!)

                        return
                    }
                }
            }
        }
    }

    private fun writeData() {
        var clipText = readClipboard()
        if (isSendEnter) {
            clipText += "\n"
        }
        Log.d("MainActivity", "on clipboard : $clipText")
        if (clipText != "") {
            ct!!.writeMessage(clipText)
            try {
                Thread.sleep(200)
                ct!!.cancel()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                ct = null
            }
        }
    }

    private fun readClipboard(): String {
        val clipboard = appContext!!.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val item = clip.getItemAt(clip.itemCount - 1)
            return item.text.toString()
        }
        return ""
    }

    fun DiscoverAvailableDevices(adapter: ArrayAdapter<String>, otherDevices: ArrayAdapter<BluetoothDevice>) {
        val mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND == action) {
                    // Get the BluetoothDevice object from the Intent
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    // Add the name and address to an array adapter to show in a ListView
                    //btDevice = device;
                    adapter.add(device?.name)// + "\n" + device.getAddress());
                    otherDevices.add(device)
                    adapter.notifyDataSetChanged()
                }
            }
        }
        // Register the BroadcastReceiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter) // Don't forget to unregister during onDestroy
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)

    }


    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {
        private val otherDevices = ArrayList<BluetoothDevice>()
        private val ct: ConnectThread? = null
        private var pairedDevices: Set<BluetoothDevice>? = null
        private var showPwdCheckBox: CheckBox? = null
        private var siteSpinner: Spinner? = null
        // clearbutton seems to always work when the gv is NOT static.
        private var gv: GridView? = null
        private var up: UserPath? = null


        private fun GetPairedDevices(btAdapter: BluetoothAdapter): Set<BluetoothDevice> {

            val pairedDevices = btAdapter.bondedDevices
            // If there are paired devices
            if (pairedDevices.size > 0) {
                // Loop through paired devices
                for (device in pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    adapter!!.add(device.name)// + "\n" + device.getAddress());
                }
                adapter!!.notifyDataSetChanged()
            }
            return pairedDevices
        }

        private fun clearClipboard() {
            val clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            //android.content.ClipData clip = android.content.ClipData.newPlainText("", "");
            val clip = android.content.ClipData.newPlainText(null, null)
            clipboard.setPrimaryClip(clip)
        }

        private fun readClipboard(): String {
            val clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(clip.itemCount - 1)
                return item.text.toString()
            }
            return ""
        }

        fun loadCurrentDeviceName() {
            val devicePrefs = MainActivity.appContext!!.getSharedPreferences("deviceName", Context.MODE_PRIVATE)
            MainActivity.btCurrentDeviceName = devicePrefs.getString("deviceName", "")

        }

        fun saveDeviceNamePref() {
            val devicePrefs = appContext!!.getSharedPreferences("deviceName", Context.MODE_PRIVATE)
            val edit = devicePrefs.edit()
            edit.putString("deviceName", MainActivity.btCurrentDeviceName)
            edit.commit()
            //PlaceholderFragment.loadSitesFromPrefs(v);
        }

        private fun setSettingsValues() {

            addUpperCaseTabCheckBox = rootView!!.findViewById(R.id.addUCaseTabCheckBox) as CheckBox
            addCharsTabCheckBox = rootView!!.findViewById(R.id.addCharsTabCheckBox) as CheckBox
            maxLengthTabCheckBox = rootView!!.findViewById(R.id.maxLengthTabCheckBox) as CheckBox
            maxLengthTabEditText = rootView!!.findViewById(R.id.maxLengthTabEditText) as EditText

             currentSiteKey.isHasSpecialChars.let{
                 addCharsTabCheckBox!!.isChecked =currentSiteKey.isHasSpecialChars;
            }
             currentSiteKey.isHasUpperCase.let{
                 addUpperCaseTabCheckBox!!.isChecked =currentSiteKey.isHasUpperCase;
            }
            maxLengthTabCheckBox!!.isChecked = currentSiteKey.maxLength > 0
            addCharsTabCheckBox!!.jumpDrawablesToCurrentState()
            addUpperCaseTabCheckBox!!.jumpDrawablesToCurrentState()
            maxLengthTabCheckBox!!.jumpDrawablesToCurrentState()
            if (currentSiteKey.maxLength > 0) {
                maxLengthTabEditText!!.setText("${currentSiteKey.maxLength}")
            }
        }

        private fun editSite() {
            val li = LayoutInflater.from(context)
            val v = li.inflate(R.layout.sitelist_dialog_main, null)

            val builder = AlertDialog.Builder(v.getContext())

            builder.setMessage("Edit Site").setCancelable(false)
                .setPositiveButton("OK") { dialog, id ->
                    val sites = MainActivity.appContext!!.getSharedPreferences("sites", Context.MODE_PRIVATE)
                    var outValues = sites.getString("sites", "")
                    sites.getString("sites", "")?.let { Log.d("MainActivity", it) }
                    val edit = sites.edit()

                    val ucCheckBox = v.findViewById(R.id.addUppercaseCheckBox) as CheckBox
                    val specCharsCheckBox = v.findViewById(R.id.addSpecialCharsCheckBox) as CheckBox
                    val maxLengthCheckBox = v.findViewById(R.id.setMaxLengthCheckBox) as CheckBox
                    val maxLengthEditText = v.findViewById(R.id.maxLengthEditText) as EditText

                    val originalLocation = allSiteKeys!!.indexOf(currentSiteKey)
                    val localSiteKey = allSiteKeys!!.get(originalLocation);

                    Log.d("MainActivity", "originalLocation : $originalLocation")
                    allSiteKeys!!.removeAt(originalLocation)

                    val input = v.findViewById(R.id.siteText) as EditText
                    val currentValue = input.text.toString()

                    currentSiteKey = SiteKey(
                        currentValue,
                        localSiteKey.isHasSpecialChars,
                        localSiteKey.isHasUpperCase,
                        localSiteKey.maxLength
                        //if (maxLengthCheckBox.isChecked) Integer.parseInt(maxLengthEditText.text.toString()) else 0
                    )

                    allSiteKeys!!.add(originalLocation, currentSiteKey!!)
                    spinnerAdapter!!.notifyDataSetChanged()
                    val gson = GsonBuilder().disableHtmlEscaping().create()
                    outValues = gson.toJson(allSiteKeys, allSiteKeys!!.javaClass)

                    edit.putString("sites", outValues)
                    edit.commit()
                    Log.d("MainActivity", "final outValues : " + outValues!!)
                    PlaceholderFragment.loadSitesFromPrefs(v)
                    siteSpinner!!.setSelection(findSiteSpinnerItemByText(currentValue), true)

                    setSettingsValues()
                }
                .setNegativeButton("CANCEL") { dialog, id -> }

            val ucCheckBox = v.findViewById(R.id.addUppercaseCheckBox) as CheckBox
            val specCharsCheckBox = v.findViewById(R.id.addSpecialCharsCheckBox) as CheckBox
            val maxLengthCheckBox = v.findViewById(R.id.setMaxLengthCheckBox) as CheckBox
            val maxLengthEditText = v.findViewById(R.id.maxLengthEditText) as EditText
            Log.d("MainActivity", "key 3 : ${currentSiteKey!!.key}")
            Log.d("MainActivity", "maxLength 3 : ${currentSiteKey!!.maxLength}")
            val input = v.findViewById(R.id.siteText) as EditText
            input.setText(currentSiteKey!!.toString())

            Log.d("MainActivity", "EDIT!")
            ucCheckBox.isChecked = currentSiteKey!!.isHasUpperCase
            Log.d("MainActivity", "uppercase : " + java.lang.Boolean.valueOf(currentSiteKey!!.isHasUpperCase))
            specCharsCheckBox.isChecked = currentSiteKey!!.isHasSpecialChars
            maxLengthCheckBox.isChecked = currentSiteKey!!.maxLength > 0
            if (currentSiteKey!!.maxLength > 0) {
                maxLengthEditText.setText("${currentSiteKey!!.maxLength}")
            }
            val alert = builder.create()
            alert.setView(v)
            alert.show()
        }

        private fun addNewSite() {

            val li = LayoutInflater.from(context)
            val v = li.inflate(R.layout.sitelist_dialog_main, null)

            val builder = AlertDialog.Builder(v.getContext())

            builder.setMessage("Add new site").setCancelable(false)
                .setPositiveButton("OK") { dialog, id ->
                    val sites = MainActivity.appContext!!.getSharedPreferences("sites", Context.MODE_PRIVATE)
                    var outValues = sites.getString("sites", "")
                    sites.getString("sites", "")?.let { Log.d("MainActivity", it) }
                    val edit = sites.edit()

                    val ucCheckBox = v.findViewById(R.id.addUppercaseCheckBox) as CheckBox
                    val specCharsCheckBox = v.findViewById(R.id.addSpecialCharsCheckBox) as CheckBox
                    val maxLengthCheckBox = v.findViewById(R.id.setMaxLengthCheckBox) as CheckBox
                    val maxLengthEditText = v.findViewById(R.id.maxLengthEditText) as EditText

                    //edit.clear();

                    val input = v.findViewById(R.id.siteText) as EditText
                    val currentValue = input.text.toString()

                    currentSiteKey = SiteKey(
                        currentValue,
                        specCharsCheckBox.isChecked,
                        ucCheckBox.isChecked,
                        if (maxLengthCheckBox.isChecked) Integer.parseInt(maxLengthEditText.text.toString()) else 0
                    )

                    allSiteKeys!!.add(currentSiteKey!!)
                    val gson = GsonBuilder().disableHtmlEscaping().create()
                    outValues = gson.toJson(allSiteKeys, allSiteKeys!!.javaClass)
                    edit.putString("sites", outValues)
                    edit.commit()
                    Log.d("MainActivity", "final outValues : " + outValues!!)
                    PlaceholderFragment.loadSitesFromPrefs(v)

                    siteSpinner!!.setSelection(findSiteSpinnerItemByText(currentValue), true)

                    setSettingsValues()
                }
                .setNegativeButton("CANCEL") { dialog, id -> }
            val alert = builder.create()
            alert.setView(v)
            alert.show()
        }

        private fun findSiteSpinnerItemByText(currentSiteKey: String): Int {
            Log.d("MainActivity", currentSiteKey)
            for (x in 0 until spinnerAdapter!!.getCount()) {
                if (spinnerAdapter!!.getItem(x).toString()!!.equals(currentSiteKey)) {
                    Log.d("MainActivity", spinnerAdapter!!.getItem(x)!!.toString())
                    return x
                }
            }
            return 0
        }

        override fun onStart() {
            super.onStart()
            Log.d("MainActivity", "onStart : " + arguments!!.getInt(ARG_SECTION_NUMBER))
            when (arguments!!.getInt(ARG_SECTION_NUMBER)) {
                1 -> {
                    addUpperCaseTabCheckBox = settingsView!!.findViewById(R.id.addUCaseTabCheckBox) as CheckBox
                    addCharsTabCheckBox = settingsView!!.findViewById(R.id.addCharsTabCheckBox) as CheckBox
                    maxLengthTabCheckBox = settingsView!!.findViewById(R.id.maxLengthTabCheckBox) as CheckBox
                    maxLengthTabEditText = settingsView!!.findViewById(R.id.maxLengthTabEditText) as EditText
                }
                2 -> {
                    addUpperCaseTabCheckBox = rootView!!.findViewById(R.id.addUCaseTabCheckBox) as CheckBox
                    addCharsTabCheckBox = rootView!!.findViewById(R.id.addCharsTabCheckBox) as CheckBox
                    maxLengthTabCheckBox = rootView!!.findViewById(R.id.maxLengthTabCheckBox) as CheckBox
                    maxLengthTabEditText = rootView!!.findViewById(R.id.maxLengthTabEditText) as EditText
                }
            }
            if (currentSiteKey != null) {
                if (addUpperCaseTabCheckBox != null) {
                    addUpperCaseTabCheckBox!!.isChecked = currentSiteKey!!.isHasUpperCase
                }
                if (addCharsTabCheckBox != null) {
                    addCharsTabCheckBox!!.isChecked = currentSiteKey!!.isHasSpecialChars
                }
                if (maxLengthTabCheckBox != null) {
                    maxLengthTabCheckBox!!.isChecked = currentSiteKey!!.maxLength > 0
                }
            }
            addCharsTabCheckBox!!.jumpDrawablesToCurrentState()
            addUpperCaseTabCheckBox!!.jumpDrawablesToCurrentState()
            maxLengthTabCheckBox!!.jumpDrawablesToCurrentState()
        }

        override fun onPause() {
            super.onPause()
            if (gv != null) {
                Log.d("MainActivity", "app is pausing")
                up = gv!!.userPath
            }
        }

        override fun onResume() {
            super.onResume()
            if (gv == null) {
                gv = GridView(appContext!!)
            }
            if (up != null) {
                gv!!.userPath = up!!
            }
            Log.d("MainActivity", "onResume : " + arguments!!.getInt(ARG_SECTION_NUMBER))
            when (arguments!!.getInt(ARG_SECTION_NUMBER)) {
                1 -> {
                    addUpperCaseTabCheckBox = settingsView!!.findViewById(R.id.addUCaseTabCheckBox) as CheckBox
                    addCharsTabCheckBox = settingsView!!.findViewById(R.id.addCharsTabCheckBox) as CheckBox
                    maxLengthTabCheckBox = settingsView!!.findViewById(R.id.maxLengthTabCheckBox) as CheckBox
                    maxLengthTabEditText = settingsView!!.findViewById(R.id.maxLengthTabEditText) as EditText
                    importSiteKeysButton = settingsView!!.findViewById(R.id.importSiteKeysButton) as Button
                }
                2 -> {
                    addUpperCaseTabCheckBox = rootView!!.findViewById(R.id.addUCaseTabCheckBox) as CheckBox
                    addCharsTabCheckBox = rootView!!.findViewById(R.id.addCharsTabCheckBox) as CheckBox
                    maxLengthTabCheckBox = rootView!!.findViewById(R.id.maxLengthTabCheckBox) as CheckBox
                    maxLengthTabEditText = rootView!!.findViewById(R.id.maxLengthTabEditText) as EditText
                    importSiteKeysButton = rootView!!.findViewById(R.id.importSiteKeysButton) as Button
                }
            }
            if (currentSiteKey != null) {
                if (addUpperCaseTabCheckBox != null) {
                    addUpperCaseTabCheckBox!!.isChecked = currentSiteKey!!.isHasUpperCase
                }
                if (addCharsTabCheckBox != null) {
                    addCharsTabCheckBox!!.isChecked = currentSiteKey!!.isHasSpecialChars
                }
                if (maxLengthTabCheckBox != null) {
                    maxLengthTabCheckBox!!.isChecked = currentSiteKey!!.maxLength > 0
                }
            }
            addCharsTabCheckBox!!.jumpDrawablesToCurrentState()
            addUpperCaseTabCheckBox!!.jumpDrawablesToCurrentState()
            maxLengthTabCheckBox!!.jumpDrawablesToCurrentState()
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            loadCurrentDeviceName()

            //final GridView gv = new us.raddev.com.cyapass.cyapass.GridView(rootView.getContext());
            gv = app.actionmobile.cyapass.GridView(appContext!!)

            rootView = inflater.inflate(R.layout.fragment_main, container, false)

            //rootView.setWillNotDraw(false);
            val mainlayout1 = rootView!!.findViewById(R.id.drawcross) as LinearLayout
            mainlayout1.addView(gv, gv!!.cellSize * 7, gv!!.cellSize * 7)
            //container.setWillNotDraw(false);

            val clearGridButton: Button
            val btDeviceSpinner: Spinner

            when (arguments!!.getInt(ARG_SECTION_NUMBER)) {
                1 -> {
                    passwordText = rootView!!.findViewById(R.id.password) as TextView
                    siteSpinner = rootView!!.findViewById(R.id.siteSpinner) as Spinner
                    showPwdCheckBox = rootView!!.findViewById(R.id.showPwd) as CheckBox
                    showPwdCheckBox!!.isChecked = true
                    clearGridButton = rootView!!.findViewById(R.id.clearGrid) as Button
                    val deleteSiteButton = rootView!!.findViewById(R.id.deleteSite) as Button
                    val addSiteButton = rootView!!.findViewById(R.id.addSite) as Button
                    loadSitesFromPrefs(rootView!!)
                    siteSpinner!!.adapter = spinnerAdapter

                    settingsView = inflater.inflate(R.layout.fragment_settings, container, false)
                    addUpperCaseTabCheckBox = settingsView!!.findViewById(R.id.addUCaseTabCheckBox) as CheckBox
                    addCharsTabCheckBox = settingsView!!.findViewById(R.id.addCharsTabCheckBox) as CheckBox
                    maxLengthTabCheckBox = settingsView!!.findViewById(R.id.maxLengthTabCheckBox) as CheckBox
                    maxLengthTabEditText = settingsView!!.findViewById(R.id.maxLengthTabEditText) as EditText
                    hidePatternCheckbox = rootView!!.findViewById(R.id.hidePatternCheckBox) as CheckBox
                    //2019-12-13 -- is it necessary to call this twice? loadSitesFromPrefs(rootView!!)

                    addSiteButton.requestFocus()

                    siteSpinner!!.setOnLongClickListener(View.OnLongClickListener {
                        currentSiteKey = siteSpinner!!.selectedItem as SiteKey
                        Log.d("MainActivity", "key 2 : ${currentSiteKey!!.key}")
                        Log.d("MainActivity", "maxLength 2 : ${currentSiteKey!!.maxLength}")
                        if (currentSiteKey!!.toString().equals("select site")) {
                            return@OnLongClickListener false
                        }
                        Log.d("MainActivity", "LONGCLICK!!!")
                        Log.d("MainActivity", currentSiteKey!!.key)
                        editSite()
                        true
                    })
                    siteSpinner!!.onItemSelectedListener =  object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?,
                                    view: View?, position: Int, id: Long
                        ) {
                            if (siteSpinner!!.selectedItemPosition <= 0) {
                                currentSiteKey = SiteKey("")
                                gv!!.ClearGrid()
                                gv!!.invalidate()

                                passwordText!!.text = ""
                                password = ""

                                clearClipboard()


                                return
                            }

                            currentSiteKey = siteSpinner!!.selectedItem as SiteKey
                            Log.d("MainActivity", "key 1 : ${currentSiteKey!!.key}")
                            Log.d("MainActivity", "maxLength 1 :${currentSiteKey!!.maxLength}")
                            addCharsTabCheckBox!!.isChecked = currentSiteKey!!.isHasSpecialChars
                            addUpperCaseTabCheckBox!!.isChecked = currentSiteKey!!.isHasUpperCase
                            if (currentSiteKey!!.maxLength > 0) {
                                maxLengthTabEditText!!.setText("${currentSiteKey!!.maxLength}")
                            }
                            maxLengthTabCheckBox!!.isChecked = currentSiteKey!!.maxLength > 0
                            if (gv!!.isLineSegmentComplete) {
                                gv!!.GeneratePassword()
                            }
                        }

                        override fun onNothingSelected(parentView: AdapterView<*>) {
                            // your code here
                        }

                    }

                    showPwdCheckBox?.setOnClickListener {
                        if (showPwdCheckBox?.isChecked!!) {
                            passwordText?.visibility = View.VISIBLE
                            passwordText?.text = password
                            Log.d("MainActivity", "password : " + password!!)
                            isPwdVisible = true
                        } else {
                            passwordText!!.text = ""
                            passwordText!!.visibility = View.INVISIBLE
                            isPwdVisible = false
                        }
                    }

                    clearGridButton.setOnClickListener {
                        gv!!.setPatternHidden(false)
                        if (hidePatternCheckbox != null) {
                            hidePatternCheckbox!!.isChecked = false
                        }

                        gv!!.ClearGrid()
                        up = null
                        gv!!.invalidate()
                        password = ""
                        passwordText!!.text = ""
                        clearClipboard()
                    }

                    addSiteButton.setOnClickListener { addNewSite() }

                    hidePatternCheckbox!!.setOnCheckedChangeListener { buttonView, isChecked ->
                        Log.d("MainActivity", "hidePatternCheckbox isChecked : $isChecked")
                        if (isChecked) {
                            gv!!.setPatternHidden(true)
                            gv!!.ClearGrid()
                            gv!!.invalidate()
                        } else {
                            gv!!.setPatternHidden(false)
                            gv!!.invalidate()
                        }
                    }

                    deleteSiteButton.setOnClickListener(View.OnClickListener { view ->
                        if (currentSiteKey == null) {
                            return@OnClickListener  // add message box need to select a valid site
                        } else {
                            AlertDialog.Builder(view.context)
                                .setTitle("Delete site?")
                                .setMessage("Are you sure you want to delete this site : ${currentSiteKey!!.toString()}?")
                                .setPositiveButton(
                                    R.string.yes_button,
                                    DialogInterface.OnClickListener { dialog, which ->
                                        spinnerItems.remove(currentSiteKey!!)
                                        spinnerAdapter!!.notifyDataSetChanged()
                                        allSiteKeys!!.remove(currentSiteKey!!)
                                        MainActivity.clearAllUserPrefs()
                                        saveUserPrefValues()

                                        siteSpinner!!.setSelection(0, true)
                                    })
                                .setNegativeButton(
                                    R.string.no_button,
                                    DialogInterface.OnClickListener { dialog, which ->
                                        // do nothing
                                    })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show()
                        }
                    })
                    addCharsTabCheckBox!!.jumpDrawablesToCurrentState()
                    addUpperCaseTabCheckBox!!.jumpDrawablesToCurrentState()
                    maxLengthTabCheckBox!!.jumpDrawablesToCurrentState()
                }
                2 -> {
                    rootView = inflater.inflate(R.layout.fragment_settings, container, false)

                    val logView: ListView
                    val listViewItems = ArrayList<String>()

                    val logViewItems = ArrayList<String>()
                    val logViewAdapter: ArrayAdapter<String>

                    val btAdapter: BluetoothAdapter?
                    val sendCtrlAltDelCheckbox: CheckBox
                    val sendEnterCheckbox: CheckBox


                    val outText: EditText
                    val specialCharsText: EditText

                    btDeviceSpinner = rootView!!.findViewById(R.id.btDevice) as Spinner
                    logView = rootView!!.findViewById(R.id.logView) as ListView

                    addUpperCaseTabCheckBox = rootView!!.findViewById(R.id.addUCaseTabCheckBox) as CheckBox
                    addCharsTabCheckBox = rootView!!.findViewById(R.id.addCharsTabCheckBox) as CheckBox
                    maxLengthTabCheckBox = rootView!!.findViewById(R.id.maxLengthTabCheckBox) as CheckBox
                    maxLengthTabEditText = rootView!!.findViewById(R.id.maxLengthTabEditText) as EditText
                    specialCharsText = rootView!!.findViewById(R.id.specialCharsTabTextBox) as EditText
                    sendCtrlAltDelCheckbox = rootView!!.findViewById(R.id.sendCtrlAltDel) as CheckBox
                    sendEnterCheckbox = rootView!!.findViewById(R.id.sendEnter) as CheckBox
                    importSiteKeysButton = rootView!!.findViewById(R.id.importSiteKeysButton) as Button

                    sendEnterCheckbox.isChecked = true
                    maxLengthTabEditText!!.setText("32")
                    addUpperCaseTabCheckBox!!.requestFocus()

                    adapter = ArrayAdapter(rootView!!.context, android.R.layout.simple_list_item_1, listViewItems)
                    btDeviceSpinner.adapter = adapter

                    logViewAdapter = ArrayAdapter(rootView!!.context, android.R.layout.simple_list_item_1, logViewItems)
                    logView.adapter = logViewAdapter

                    btAdapter = BluetoothAdapter.getDefaultAdapter()
                    if (btAdapter != null) {
                        if (!btAdapter.isEnabled) {
                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                        }
                        pairedDevices = GetPairedDevices(btAdapter)
                        //DiscoverAvailableDevices();
                    }

                    importSiteKeysButton!!.setOnClickListener {
                        Log.d("MainActivity", "import button clicked!")
                        displayImportDialog()
                    }

                    sendEnterCheckbox.setOnClickListener {
                        if (sendEnterCheckbox.isChecked) {
                            MainActivity.isSendEnter = true
                        } else {
                            MainActivity.isSendEnter = false
                        }
                    }

                    sendCtrlAltDelCheckbox.setOnClickListener {
                        if (sendCtrlAltDelCheckbox.isChecked) {
                            MainActivity.isSendCtrlAltDel = true
                        } else {
                            MainActivity.isSendCtrlAltDel = false
                        }
                    }

                    addUpperCaseTabCheckBox!!.setOnCheckedChangeListener { buttonView, isChecked ->

                        if (isChecked) {
                            currentSiteKey!!.isHasUpperCase =true
                        } else {
                            currentSiteKey!!.isHasUpperCase = false
                        }
                        if (gv!!.isLineSegmentComplete) {
                            Log.d("MainActivity", "addChars -- Re-generating password...")
                            gv!!.GeneratePassword()
                        }
                    }

                    addCharsTabCheckBox!!.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            currentSiteKey!!.isHasSpecialChars = true
                        } else {
                            currentSiteKey!!.isHasSpecialChars = false
                        }
                        if (gv!!.isLineSegmentComplete) {
                            Log.d("MainActivity", "addChars -- Re-generating password...")
                            gv!!.GeneratePassword()
                        }
                    }

                    maxLengthTabCheckBox!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                        if (currentSiteKey == null) {
                            return@OnCheckedChangeListener
                        }
                        if (isChecked) {
                            currentSiteKey!!.maxLength = Integer.parseInt(maxLengthTabEditText!!.text.toString())
                        } else {
                            currentSiteKey!!.maxLength = 0
                        }
                        if (gv!!.isLineSegmentComplete) {
                            Log.d("MainActivity", "addChars -- Re-generating password...")
                            gv!!.GeneratePassword()
                        }
                    })

                    specialCharsText.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable) {
                            if (currentSiteKey == null) {
                                return
                            }
                            MainActivity.specialChars = s.toString()
                            if (currentSiteKey!!.isHasSpecialChars) {
                                gv!!.GeneratePassword()
                            }
                        }

                        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                            // TODO Auto-generated method stub

                        }

                        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                        }

                    })
                    maxLengthTabEditText!!.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            if (s != null) {
                                if (s.length > 0) {
                                    MainActivity.maxLength = Integer.parseInt(s.toString())
                                    if (isMaxLength) {
                                        gv!!.GeneratePassword()
                                    }
                                }

                            }
                        }

                        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                            // TODO Auto-generated method stub

                        }

                        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                        }

                    })

                    btDeviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parentView: AdapterView<*>,
                            selectedItemView: View,
                            position: Int,
                            id: Long
                        ) {
                            btCurrentDeviceName = btDeviceSpinner.selectedItem.toString()
                            saveDeviceNamePref()
                            Log.d("MainActivity", "DeviceInfo : " + btCurrentDeviceName!!)
                            logViewAdapter.add("DeviceInfo : " + btCurrentDeviceName!!)
                            logViewAdapter.notifyDataSetChanged()
                        }

                        override fun onNothingSelected(parentView: AdapterView<*>) {
                            // your code here
                        }
                    }

                    InitializeDeviceSpinner(btDeviceSpinner)
                }
            }

            return rootView
        }

        fun displayImportDialog(){
            val li = LayoutInflater.from(context)
            val v = li.inflate(R.layout.import_dialog_main, null)

            val builder = AlertDialog.Builder(v.getContext())
            val queue = Volley.newRequestQueue(context)
            var currentValue : String = ""
            var secretId = v.findViewById(R.id.cyaSecretId) as EditText

            // Force regeneration of password to insure all changes user has made are applied.
            gv!!.GeneratePassword()

            builder.setMessage("Import SiteKeys").setCancelable(false)
                .setPositiveButton("OK") { dialog, id ->

                    var url = v.findViewById(R.id.siteKeyListUrl) as EditText
                    var secretId = v.findViewById(R.id.cyaSecretId) as EditText
                    var targetUrl : String = url.text.toString();
                    targetUrl += "Cya/GetData?key=" + secretId.text;
                    Log.d("MainActivity", targetUrl);

// Request a string response from the provided URL.
                    val stringRequest = StringRequest(
                        Request.Method.GET, targetUrl,
                        { response ->
                            Log.d("MainActivity", "URL returned...")
                            Log.d("MainActivity","Response is: ${response}")
                            val gson = Gson()
                            try {
                                Log.d("MainActivity", "Deserialize LibreStore JSON.")

                                var libreStore = gson.fromJson<Any>(response, object : TypeToken<LibreStoreJson>(){

                                }.type) as LibreStoreJson;
                                //var bucket = gson.fromJson<Any>(testThing.)
                                if (libreStore.success == false){
                                    Log.d("MainActivity","message: ${libreStore.message}")
                                    val alertDialog =
                                        context?.let {
                                            AlertDialog.Builder(it).create()
                                        }
                                    alertDialog?.setTitle("Import Failed")
                                    alertDialog?.setMessage("${libreStore.message}\nPlease set a valid CYa Secret Id & try again.")
                                    alertDialog?.setButton(
                                        AlertDialog.BUTTON_NEUTRAL, "OK"
                                    ) { dialog, which -> dialog.dismiss() }
                                    alertDialog?.show()

                                    throw Exception("failed")
                                }
                                Log.d("MainActivity", libreStore.success.toString())
                                Log.d("MainActivity", libreStore.cyabucket.data)
                                Log.d("MainActivity", "ClearTextPwd: ${gv!!.ClearTextPwd}")

                                var c = Crypton("${gv!!.ClearTextPwd}",
                                    Base64.decode(libreStore.cyabucket.data,Base64.DEFAULT))
                                var decryptedData = c.processData(false)
                                //processData on error - returns a string which includes "Decryption failed"
                                Log.d("MainActivity",decryptedData.substring(0..16))

                                if (decryptedData.substring(0..16) == "Decryption failed"){
                                    val alertDialog =
                                        context?.let {
                                            AlertDialog.Builder(it).create()
                                        }
                                    alertDialog?.setTitle("Decryption Failed")
                                    alertDialog?.setMessage("Could not decrypt data.\nPlease set a valid Password (pattern & sitekey) & try again.")
                                    alertDialog?.setButton(
                                        AlertDialog.BUTTON_NEUTRAL, "OK"
                                    ) { dialog, which -> dialog.dismiss() }
                                    alertDialog?.show()
                                    throw Exception("Decryption failure: incorrect password!")
                                }
                                val keysAddedCount = deserializeSiteKeys(decryptedData)
                                val text = "Success! Imported ${keysAddedCount} new keys."
                                val duration = Toast.LENGTH_LONG
                                Toast.makeText(context, text, duration)
                                    .show()
                            }
                            catch (x: Exception) {
                                x.message?.let { Log.d("MainActivity", it) }
                            }
                        },
                        {
                            Log.d("MainActivity", "That didn't work!")
                            val text = "Failed to import keys! Error: ${it.message}"
                            val duration = Toast.LENGTH_LONG
                            Toast.makeText(context, text, duration)
                                .show()
                        })

// Add the request to the RequestQueue.
                    queue.add(stringRequest)

                }
                .setNegativeButton("CANCEL") { dialog, id -> }
            val alert = builder.create()
            alert.setView(v)
            alert.show()
        }

        fun deserializeSiteKeys(sites: String): Int{
            //remove the "select site" item every time -- it's added back later
            if (spinnerAdapter!!.count > 0) {
                spinnerAdapter!!.remove(spinnerAdapter!!.getItem(0))
            }
            val gson = Gson()
            try {
                Log.d("MainActivity", "Attempting deserialization of JSON.")
                allSiteKeys!!.clear()
                allSiteKeys = gson.fromJson<Any>(sites, object : TypeToken<List<SiteKey>>()
                {

                }.type) as MutableList<SiteKey>

                // if item is in spinner but not in allSiteKeys
                // then add it back into allSiteKeys

                // if item is already in spinner add it into allSiteKeys
                for (itemCounter in 0..spinnerAdapter!!.count-1){
                    var x = allSiteKeys!!.find{
                        it.toString().equals(spinnerAdapter!!.getItem(itemCounter).toString())
                    }
                    if (x == null) {
                        spinnerAdapter!!.getItem(itemCounter)?.let { allSiteKeys!!.add(it) };
                        Log.d("MainActivity", "Items in spinner -->  ${spinnerAdapter!!.getItem(itemCounter).toString()}")
                    }
                }

                Log.d("MainActivity", "Deserialization SUCCESS!")
                Log.d("MainActivity", "There are ${allSiteKeys!!.size} sitekeys.")
                if (allSiteKeys == null) {
                    allSiteKeys = ArrayList<SiteKey>()
                }

                var isFound : Boolean = false
                var keysAddedCount = 0;
                for (sk in allSiteKeys!!) {
                    isFound = false
                    Log.d("MainActivity","spinnerAdapter!!.count : ${spinnerAdapter!!.count}")
                    for (i in 0..spinnerAdapter!!.count-1){
                        Log.d("MainActivity", "i : ${i}")
                        if (spinnerAdapter!!.getItem(i)!!.key.equals(sk.key)) {
                            Log.d("MainActivity","Found item: ${sk.key.toString()}" )
                            isFound = true;
                            continue
                        }
                    }
                    if (!isFound) {
                        spinnerAdapter!!.add(sk)
                        keysAddedCount++
                    }
                }
                spinnerAdapter!!.sort { a1, a2 -> a1.toString().compareTo(a2.toString(), true) }
                Log.d("MainActivity", "getItem(0) -> ${spinnerAdapter!!.getItem(0)?.toString()}")
                spinnerAdapter!!.insert(SiteKey("select site"), 0)

                spinnerAdapter!!.notifyDataSetChanged()
                MainActivity.clearAllUserPrefs()
                SaveValuesToPrefs();
                return keysAddedCount
            } catch (x: Exception) {
                x.message?.let { Log.d("MainActivity", it) }
            }
            return 0;
        }

        fun SaveValuesToPrefs(){
            val sites = MainActivity.appContext!!.getSharedPreferences("sites", Context.MODE_PRIVATE)

            Log.d("MainActivity", "saveValuesToPrefs ${sites.getString("sites", "")}")
            val edit = sites.edit()
            val gson = GsonBuilder().disableHtmlEscaping().create()
            val outValues = gson.toJson(allSiteKeys, allSiteKeys!!.javaClass)
            Log.d("MainActivity", "outValues -> ${outValues}")
            edit.putString("sites", outValues).apply()
            edit.commit()

        }

        fun InitializeDeviceSpinner(btDeviceSpinner: Spinner) {
            if (btCurrentDeviceName != null && btCurrentDeviceName !== "") {
                var counter = 0
                while (counter < adapter!!.count) {
                    Log.d("MainActivity", "adapter.getItem : " + adapter!!.getItem(counter)!!.toString())
                    if (adapter!!.getItem(counter).toString() == btCurrentDeviceName) {
                        break
                    }
                    counter++
                }
                btDeviceSpinner.setSelection(counter)
            }
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private val ARG_SECTION_NUMBER = "section_number"
            internal val REQUEST_ENABLE_BT = 1
            private var adapter: ArrayAdapter<String>? = null
            public var password: String? = null
            private val spinnerItems = ArrayList<SiteKey>()
            private var spinnerAdapter: ArrayAdapter<SiteKey>? = null
            private var rootView: View? = null
            private var settingsView: View? = null

            private var addCharsTabCheckBox: CheckBox? = null
            private var addUpperCaseTabCheckBox: CheckBox? = null
            private var maxLengthTabCheckBox: CheckBox? = null
            private var maxLengthTabEditText: EditText? = null
            private var importSiteKeysButton: Button? = null

            internal var hidePatternCheckbox: CheckBox? = null

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }

            fun loadSitesFromPrefs(vx: View) {
                Log.d("MainActivity", "Loading sites from preferences")

                val sitePrefs = MainActivity.appContext!!.getSharedPreferences("sites", Context.MODE_PRIVATE)
                initializeSpinnerAdapter(vx)
                val sites = sitePrefs.getString("sites", "")
                Log.d("MainActivity", "sites : ${sites}")
//                #### following two lines allow me to delete all sitekeys
//                sitePrefs.edit().clear().apply()
//                sitePrefs.edit().commit()
                val gson = Gson()
                try {
                    allSiteKeys = gson.fromJson<Any>(sites, object : TypeToken<List<SiteKey>>()
                    {

                    }.type) as MutableList<SiteKey>
                    Log.d("MainActivity", "allSiteKeys.size : ${allSiteKeys!!.size}")
                    if (allSiteKeys == null) {
                        allSiteKeys = ArrayList<SiteKey>()
                    }

                    for (sk in allSiteKeys!!) {
                        spinnerAdapter!!.add(sk)
                    }
                    spinnerAdapter!!.sort { a1, a2 -> a1.toString().compareTo(a2.toString(), true) }
                    if (!spinnerAdapter!!.getItem(0)?.toString().equals("select site")) {
                        spinnerAdapter!!.insert(SiteKey("select site"), 0)
                    }

                    spinnerAdapter!!.notifyDataSetChanged()
                } catch (x: Exception) {
                    x.message?.let { Log.d("MainActivity", it) }
                    val allSites = sites!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    Log.d("MainActivity", "sites : $sites")
                    Log.d("MainActivity", "Reading items from prefs")
                    for (s in allSites) {
                        Log.d("MainActivity", "s : $s")
                        if (s !== "") {
                            spinnerAdapter!!.add(SiteKey(s))
                        }
                    }
                }

            }

            private fun initializeSpinnerAdapter(v: View) {
                if (spinnerAdapter == null) {
                    spinnerAdapter = ArrayAdapter<SiteKey>(v.context, android.R.layout.simple_list_item_1, spinnerItems)
                }
                spinnerAdapter!!.clear()
                //spinnerAdapter.add(new SiteKey("select site"));
                spinnerAdapter!!.sort { a1, a2 -> a1.toString().compareTo(a2.toString(), true) }
                spinnerAdapter!!.notifyDataSetChanged()

            }
        }
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1)
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "MAIN"
                1 -> return "SETTINGS"
            }
            return null
        }
    }

    companion object {
        internal var REQUEST_ENABLE_BT = 1
        private var appContext: Context? = null
        private var passwordText: TextView? = null
        private val password: String? = null
        private var isPwdVisible = true
        var isAddUppercase = false
        var isAddSpecialChars = false
        var isMaxLength = false
        var btCurrentDeviceName: String? = null
        var isSendCtrlAltDel = false
        var isSendEnter = true
        internal var specialChars: String? = null
        internal var maxLength = 32

        private var allSiteKeys: MutableList<SiteKey>? = mutableListOf<SiteKey>()
        var currentSiteKey: SiteKey = SiteKey("")

        fun saveUserPrefValues() {
            val sites = MainActivity.appContext!!.getSharedPreferences("sites", Context.MODE_PRIVATE)
            var outValues = sites.getString("sites", "")
            sites.getString("sites", "")?.let { Log.d("MainActivity", it) }
            val edit = sites.edit()

            val gson = Gson()
            outValues = SiteKey.toJson(allSiteKeys as List<SiteKey>)

            edit.putString("sites", outValues)
            edit.commit()
            Log.d("MainActivity", "final outValues : " + outValues!!)
        }

        fun clearAllUserPrefs() {
            Log.d("MainActivity", "### clearAllUserPrefs() ###")
            val sites = appContext!!.getSharedPreferences("sites", Context.MODE_PRIVATE)
            Log.d("MainActivity", "sites before: ${sites.getString("sites","")}")
            val edit = sites.edit()
            edit.clear().apply()
            edit.commit()
            Log.d("MainActivity", "sites after: ${sites.getString("sites","")}")

            //PlaceholderFragment.loadSitesFromPrefs(v);
        }

        fun SetPassword(pwd: String) {
            Log.d("MainActivity", "pwd: $pwd")
            PlaceholderFragment.password = pwd
            if (isPwdVisible) {
                passwordText!!.text = pwd
            }
        }
    }
}
