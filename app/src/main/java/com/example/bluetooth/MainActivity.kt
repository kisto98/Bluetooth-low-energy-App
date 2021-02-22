package com.example.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.companion.BluetoothDeviceFilter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.bluetooth.ConnectionManager.connect
import com.punchthrough.blestarterappandroid.ScanResultAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.*
import kotlinx.android.synthetic.main.row_scan_result.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.bluetoothManager
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private val ENABLE_BLUETOOTH_REQUEST_CODE = 1 //check if b is on
    private val LOCATION_PERMISSION_REQUEST_CODE = 2
    private val bleScanner by lazy { bluetoothAdapter.bluetoothLeScanner }


    //uvodjenje gatt
    private var connectedDeviceMap: MutableMap<String, BluetoothGatt>? = null

    //
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bluetoothDevice: BluetoothDevice by lazy { bluetoothDevice }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    // location premision
    val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            alert {
                title = "Location permission required"
                message = "Starting from Android M (6.0), the system requires apps to be granted " +
                        "location access in order to scan for BLE devices."
                isCancelable = false
                positiveButton(android.R.string.ok) {
                    requestPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }.show()
        }
    }

    //
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startBleScan()
                }
            }
        }
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

//prermisions end

    // Scanning start,stop
    private fun startBleScan() {
        if (!isLocationPermissionGranted) {
            requestLocationPermission()

        } else {
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true

            conDevAdapter.notifyDataSetChanged()
            conDevAdapter.notifyItemInserted(-1)
        }
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { scan_button.text = if (value) "Stop Scan" else "Start Scan" }
        }


    private val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)


            } else {
                with(result.device) {
                    Log.i("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address", "")
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
                conDevAdapter.notifyDataSetChanged()
                conDevAdapter.notifyItemInserted(+1)

            }
        }

        private fun connectionState(status: Int): String {
            return when (status) {
                BluetoothProfile.STATE_CONNECTED -> "Connected"
                BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
                BluetoothProfile.STATE_CONNECTING -> "Connecting"
                BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
                else -> status.toString()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("onScanFailed: code $errorCode", "Error")
        }
    }

    //skrol meni
    private fun setupRecyclerView() {
        scan_results_recycler_view.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                    this@MainActivity,
                    RecyclerView.VERTICAL,
                    false
            )
            isNestedScrollingEnabled = false
        }

        val animator = scan_results_recycler_view.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
           condevs_layout.apply {
               condevs_layout?.layoutManager = LinearLayoutManager(this@MainActivity)
                 condevs_layout.adapter = ConDevAdapter(somelist)
               adapter = conDevAdapter
               //napredak
           }

    }
    private val somelist: MutableList<BluetoothDevice>? by lazy { bluetoothManager.getConnectedDevices(BluetoothProfile.GATT) }

    private val conDevAdapter: ConDevAdapter by lazy {
        ConDevAdapter(somelist)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.nav_drawer_menu, menu)
        return true
    }


    //Scan results
    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            if (isScanning) {
                stopBleScan()
            }
            with(result.device) {
                Timber.w("Connecting to $address")
                connect(this, this@MainActivity)

            }
        }

    }
    var pairedDevices = BluetoothAdapter.getDefaultAdapter().bondedDevices

    //bt connect

    override fun onResume() {
        super.onResume()
        ConnectionManager.registerListener(connectionEventListener)
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onConnectionSetupComplete = { gatt ->
                Intent(this@MainActivity, BleOperationsActivity::class.java).also {
                    it.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.device)
                    startActivity(it)
                }
                ConnectionManager.unregisterListener(this)
            }
            onDisconnect = {
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "Disconnected or unable to connect to device."
                        positiveButton("OK") {}
                    }.show()
                }
            }
        }
    }


    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    btn_iscon.text = "Disconect"
                    conDevAdapter.notifyDataSetChanged()
                    conDevAdapter.notifyItemInserted(-1)
                    somelist?.add(1, bluetoothDevice)
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    // TODO: Store a reference to BluetoothGatt
                    menu.setOnClickListener { setContentView(R.layout.activity_main); }
                    if (!connectedDeviceMap!!.containsKey(deviceAddress)) {
                        connectedDeviceMap!!.put(deviceAddress, gatt)
                    }
                    // Broadcast if needed
                    Log.i("asd", "Attempting to start service discovery:" +
                            gatt.discoverServices());

                    //

                          conDevAdapter.notifyDataSetChanged()
                          conDevAdapter.notifyItemInserted(-1)
                    ///get connected
                    if (pairedDevices.size > 0) {
                        for (d in pairedDevices) {
                            val deviceName = d.name
                            val macAddress = d.address
                            Log.w("Pairedones", "paired device: $deviceName at $macAddress")
                            // do what you need/want this these list items

                        }

                        somelist?.add(1, bluetoothDevice)

                        ///

                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                    if (connectedDeviceMap!!.containsKey(deviceAddress)) {
                        var bluetoothGatt = connectedDeviceMap!![deviceAddress]
                        if (bluetoothGatt != null) {
                            bluetoothGatt.close()
                            bluetoothGatt = null
                        }
                        connectedDeviceMap!!.remove(deviceAddress)
                    }

                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }
        }
    }
    //navmenu

    lateinit var toggle: ActionBarDrawerToggle
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        scan_button.setOnClickListener { if (isScanning) stopBleScan() else startBleScan() }
        setupRecyclerView()
        connectedDeviceMap = HashMap()
        menu.setOnClickListener { setContentView(R.layout.activity_main); }
//navmenu
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.mItem -> Toast.makeText(applicationContext, "clicked", Toast.LENGTH_SHORT).show()
            }
            true
        }

    }
}




/**
     Scan try
     **/





