package app.actionmobile.cyapass

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import android.widget.ArrayAdapter

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Created by roger.deutsch on 7/6/2016.
 */
class ConnectThread(private val mmDevice: BluetoothDevice, uuid: UUID, logViewAdapter: ArrayAdapter<String>?) :
    Thread() {
    private val mmSocket: BluetoothSocket?
    private var logViewAdapter: ArrayAdapter<String>? = null
    //private final InputStream mmInStream;
    private var mmOutStream: OutputStream? = null
    private var mmInStream: InputStream? = null

    init {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final

        var tmp: BluetoothSocket? = null
        if (logViewAdapter != null) {
            this.logViewAdapter = logViewAdapter
            logViewAdapter.add("in ConnectThread()...")
            logViewAdapter.notifyDataSetChanged()
        }
        val tmpOut: OutputStream
        val tmpIn: InputStream

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            Log.d("MainActivity", "creating RfcommSocket...")
            if (logViewAdapter != null) {
                logViewAdapter.add("creating RfcommSocket...")
                logViewAdapter.notifyDataSetChanged()
            }
            tmp = mmDevice.createRfcommSocketToServiceRecord(uuid)
            Log.d("MainActivity", "created.")
            if (logViewAdapter != null) {
                logViewAdapter.add("created")
                logViewAdapter.notifyDataSetChanged()
            }
        } catch (e: IOException) {
            Log.d("MainActivity", "FAILED! : " + e.message)
            if (logViewAdapter != null) {
                logViewAdapter.add("FAILED! : " + e.message)
                logViewAdapter.notifyDataSetChanged()
            }
        }

        mmSocket = tmp
        try {
            tmpOut = tmp!!.outputStream
            mmOutStream = tmpOut
            tmpIn = mmSocket!!.inputStream
            mmInStream = tmpIn
            //mmInStream = tmp.getInputStream();
        } catch (iox: IOException) {
            Log.d("MainActivity", "failed to get stream : " + iox.message)
        } catch (npe: NullPointerException) {
            Log.d("MainActivity", "null pointer on stream : " + npe.message)

        }

    }

    fun writeYes() {
        try {
            val outByte = byteArrayOf(121)

            mmOutStream!!.write(outByte)
            if (logViewAdapter != null) {
                logViewAdapter!!.add("Success; Wrote YES!")
                logViewAdapter!!.notifyDataSetChanged()
            }

            Thread.sleep(500)

        } catch (e: IOException) {
        } catch (e: InterruptedException) {
            Log.d("MainActivity", e.stackTrace.toString())
        }

    }

    fun writeNo() {
        try {
            val outByte = byteArrayOf(110)
            mmOutStream!!.write(outByte)
            if (logViewAdapter != null) {
                logViewAdapter!!.add("Success; Wrote NO")
                logViewAdapter!!.notifyDataSetChanged()
            }

            mmOutStream!!.write(outByte)
        } catch (e: IOException) {
        }

    }

    fun writeCtrlAltDel() {
        try {
            val outByte = byteArrayOf(0x01)
            mmOutStream!!.write(outByte)
            if (logViewAdapter != null) {
                logViewAdapter!!.add("Success; Wrote 0x01")
                logViewAdapter!!.notifyDataSetChanged()
            }
        } catch (e: IOException) {
        }

    }

    fun writeMessage(message: String) {
        try {
            var outByte = ByteArray(message.length)
            outByte = message.toByteArray()
            mmOutStream!!.write(outByte)
            //logViewAdapter.add("Success; Wrote YES!");
            if (logViewAdapter != null) {
                logViewAdapter!!.notifyDataSetChanged()
            }

        } catch (e: IOException) {
        }

    }

    fun run(btAdapter: BluetoothAdapter) {
        // Cancel discovery because it will slow down the connection
        btAdapter.cancelDiscovery()

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            Log.d("MainActivity", "Connecting...")
            if (logViewAdapter != null) {
                logViewAdapter!!.add("Connecting...")
                logViewAdapter!!.notifyDataSetChanged()
            }
            mmSocket!!.connect()
            Log.d("MainActivity", "Connected")
            if (logViewAdapter != null) {
                logViewAdapter!!.add("Connected")
                logViewAdapter!!.notifyDataSetChanged()
            }
            if (mmOutStream != null) {
                if (logViewAdapter != null) {
                    mmOutStream!!.write(byteArrayOf(65, 66))
                    logViewAdapter!!.add("Success; Wrote 2 bytes!")
                    logViewAdapter!!.notifyDataSetChanged()
                }
            }
        } catch (connectException: IOException) {
            // Unable to connect; close the socket and get out
            Log.d("MainActivity", "Failed! : " + connectException.message)
            if (logViewAdapter != null) {
                logViewAdapter!!.add("Failed! : " + connectException.message)
                logViewAdapter!!.notifyDataSetChanged()
            }
            try {
                mmSocket!!.close()
            } catch (closeException: IOException) {
            }

            return
        }

        // Do work to manage the connection (in a separate thread)
        //manageConnectedSocket(mmSocket);
    }

    override fun run() {
        val buffer = ByteArray(1024)  // buffer store for the stream
        var bytes: Int // bytes returned from read()
        if (logViewAdapter != null) {
            logViewAdapter!!.add("Reading from BT!...")
            logViewAdapter!!.notifyDataSetChanged()
        }
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream!!.read(buffer)
                // Send the obtained bytes to the UI activity
                if (logViewAdapter != null) {
                    logViewAdapter!!.add(bytes.toString())
                    logViewAdapter!!.notifyDataSetChanged()
                }
            } catch (e: IOException) {
                if (logViewAdapter != null) {
                    logViewAdapter!!.add("IOException on read: " + e.message)
                    logViewAdapter!!.notifyDataSetChanged()
                }
            }

        }
    }

    /** Will cancel an in-progress connection, and close the socket  */
    fun cancel() {
        try {
            mmSocket!!.close()
        } catch (e: IOException) {
        }

    }
}
