package app.actionmobile.cyapass

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

import java.util.ArrayList
import java.util.Comparator
import java.util.UUID
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * [FragmentPagerAdapter] derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
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
        clipboard.primaryClip = clip
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
                    Log.d("MainActivity", e.message)
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
                    adapter.add(device.name)// + "\n" + device.getAddress());
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
            clipboard.primaryClip = clip
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

            addCharsTabCheckBox!!.isChecked = currentSiteKey!!.isHasSpecialChars
            addUpperCaseTabCheckBox!!.isChecked = currentSiteKey!!.isHasUpperCase
            maxLengthTabCheckBox!!.isChecked = currentSiteKey!!.maxLength > 0
            addCharsTabCheckBox!!.jumpDrawablesToCurrentState()
            addUpperCaseTabCheckBox!!.jumpDrawablesToCurrentState()
            maxLengthTabCheckBox!!.jumpDrawablesToCurrentState()
            if (currentSiteKey!!.maxLength > 0) {
                maxLengthTabEditText!!.setText("${currentSiteKey!!.maxLength}")
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
                    Log.d("MainActivity", sites.getString("sites", ""))
                    val edit = sites.edit()

                    val ucCheckBox = v.findViewById(R.id.addUppercaseCheckBox) as CheckBox
                    val specCharsCheckBox = v.findViewById(R.id.addSpecialCharsCheckBox) as CheckBox
                    val maxLengthCheckBox = v.findViewById(R.id.setMaxLengthCheckBox) as CheckBox
                    val maxLengthEditText = v.findViewById(R.id.maxLengthEditText) as EditText

                    val originalLocation = allSiteKeys!!.indexOf(currentSiteKey)
                    Log.d("MainActivity", "originalLocation : $originalLocation")
                    allSiteKeys!!.removeAt(originalLocation)

                    val input = v.findViewById(R.id.siteText) as EditText
                    val currentValue = input.text.toString()

                    currentSiteKey = SiteKey(
                        currentValue,
                        specCharsCheckBox.isChecked,
                        ucCheckBox.isChecked,
                        maxLengthCheckBox.isChecked,
                        if (maxLengthCheckBox.isChecked) Integer.parseInt(maxLengthEditText.text.toString()) else 0
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
            input.setText(currentSiteKey!!.key)

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
                    Log.d("MainActivity", sites.getString("sites", ""))
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
                        maxLengthCheckBox.isChecked,
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
                    loadSitesFromPrefs(rootView!!)

                    addSiteButton.requestFocus()

                    siteSpinner!!.setOnLongClickListener(View.OnLongClickListener {
                        currentSiteKey = siteSpinner!!.selectedItem as SiteKey
                        Log.d("MainActivity", "key 2 : ${currentSiteKey!!.key}")
                        Log.d("MainActivity", "maxLength 2 : ${currentSiteKey!!.maxLength}")
                        if (currentSiteKey!!.key.equals("select site")) {
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
                                currentSiteKey = null
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

                    showPwdCheckBox!!.setOnClickListener {
                        if (showPwdCheckBox!!.isChecked) {
                            passwordText!!.visibility = View.VISIBLE
                            passwordText!!.text = password
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
                        val queue = Volley.newRequestQueue(it.context)
                        val url = "http://raddev.us/allsitekeys.json"

// Request a string response from the provided URL.
                        val stringRequest = StringRequest(
                            Request.Method.GET, url,
                            Response.Listener<String> { response ->
                                // Display the first 500 characters of the response string.
                                Log.d("MainActivity", "URL returned...")
                                //Log.d("MainActivity","Response is: ${response.substring(0, 500)}")
                                Log.d("MainActivity","Response is: ${response}")
                                deserializeSiteKeys(response)
                            },
                            Response.ErrorListener { Log.d("MainActivity", "That didn't work!")})

// Add the request to the RequestQueue.
                        queue.add(stringRequest)

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

        fun deserializeSiteKeys(sites: String){
            val gson = Gson()
            try {
                Log.d("MainActivity", "Attempting deserialization of JSON.")
                allSiteKeys = gson.fromJson<Any>(sites, object : TypeToken<List<SiteKey>>()
                {

                }.type) as MutableList<SiteKey>
                Log.d("MainActivity", "Deserialization SUCCESS!")
                if (allSiteKeys == null) {
                    allSiteKeys = ArrayList<SiteKey>()
                }

                for (sk in allSiteKeys!!) {
                    spinnerAdapter!!.add(sk)
                }
                spinnerAdapter!!.sort { a1, a2 -> a1.toString().compareTo(a2.toString(), true) }
                spinnerAdapter!!.insert(SiteKey("select site"), 0)

                spinnerAdapter!!.notifyDataSetChanged()
            } catch (x: Exception) {
                Log.d("MainActivity", x.message)
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
//                #### following two lines
//                sitePrefs.edit().clear().apply()
//                sitePrefs.edit().commit()
                val gson = Gson()
                try {
                    allSiteKeys = gson.fromJson<Any>(sites, object : TypeToken<List<SiteKey>>()
                    {

                    }.type) as MutableList<SiteKey>
                    if (allSiteKeys == null) {
                        allSiteKeys = ArrayList<SiteKey>()
                    }

                    for (sk in allSiteKeys!!) {
                        spinnerAdapter!!.add(sk)
                    }
                    spinnerAdapter!!.sort { a1, a2 -> a1.toString().compareTo(a2.toString(), true) }
                    spinnerAdapter!!.insert(SiteKey("select site"), 0)

                    spinnerAdapter!!.notifyDataSetChanged()
                } catch (x: Exception) {
                    Log.d("MainActivity", x.message)
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
        var currentSiteKey: SiteKey? = null

        fun saveUserPrefValues() {
            val sites = MainActivity.appContext!!.getSharedPreferences("sites", Context.MODE_PRIVATE)
            var outValues = sites.getString("sites", "")
            Log.d("MainActivity", sites.getString("sites", ""))
            val edit = sites.edit()

            val gson = Gson()
            outValues = SiteKey.toJson(allSiteKeys as List<SiteKey>)

            edit.putString("sites", outValues)
            edit.commit()
            Log.d("MainActivity", "final outValues : " + outValues!!)
        }

        fun clearAllUserPrefs() {
            val sites = appContext!!.getSharedPreferences("sites", Context.MODE_PRIVATE)
            val edit = sites.edit()
            edit.clear()
            edit.commit()
            //PlaceholderFragment.loadSitesFromPrefs(v);
        }

        fun SetPassword(pwd: String) {
            PlaceholderFragment.password = pwd
            if (isPwdVisible) {
                passwordText!!.text = pwd
            }
        }
    }
}
