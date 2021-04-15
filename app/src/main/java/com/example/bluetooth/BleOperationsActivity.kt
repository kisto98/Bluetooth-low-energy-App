/*
 * Copyright 2019 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright 2019 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth


import android.app.Activity
import android.app.Dialog
import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetooth.ConnectionManager.BSERVICELEVEL
import com.example.bluetooth.ConnectionManager.FSERVICEFIRWARE
import com.example.bluetooth.ConnectionManager.enableNotifications
import com.example.bluetooth.ConnectionManager.readCharacteristic
import com.example.bluetooth.ConnectionManager.writeCharacteristic
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.punchthrough.blestarterappandroid.ScanResultAdapter
import kotlinx.android.synthetic.main.activity_ble_operations.*
import kotlinx.android.synthetic.main.activity_ble_operations.drawerLayout
import kotlinx.android.synthetic.main.activity_ble_operations.navView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.*
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceController
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import org.jetbrains.anko.alert
import org.jetbrains.anko.bluetoothManager
import org.jetbrains.anko.toast
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class BleOperationsActivity : AppCompatActivity() {

    lateinit var device: BluetoothDevice
    private val dateFormatter = SimpleDateFormat("MMM d, HH:mm:ss", Locale.US)
    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }
    val number: UUID = UUID.fromString("adc0ca03-e16b-45c9-8980-3fe6300d5ded")

    private var notifyingCharacteristics = mutableListOf<UUID>()

    private fun setUpBottombar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.metrics
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.main -> {
                    Intent(this, MainActivity::class.java).also {
                        //  it.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        it.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                        startActivity(it)
                    }
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true

                }
                R.id.metrics -> {
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.settings -> {
                    Intent(this, SettingsActivity::class.java).also {
                        startActivity(it)
                    }
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }
            }
            false
        }
    }

    //navmenu
    lateinit var toggle: ActionBarDrawerToggle
    fun menulayout() {
        condevs_layout.apply {
            condevs_layout?.layoutManager = LinearLayoutManager(this@BleOperationsActivity)
            adapter = conDevAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.nav_drawer_menu, menu)
        menulayout()
        return true
    }
    //end navmenu

    private val conbledev: MutableList<BluetoothDevice>? by lazy {
        bluetoothManager.getConnectedDevices(
            BluetoothProfile.GATT
        )
    }
    private val conDevAdapter: ConDevAdapter by lazy {
        ConDevAdapter(conbledev) { bluetoothDevice ->
            Intent(this, BleOperationsActivity::class.java).also {
                it.putExtra(BluetoothDevice.EXTRA_DEVICE, bluetoothDevice)
                startActivity(it)

            }
            //  btn_disconnect.setOnClickListener { ConnectionManager.teardownConnection(bluetoothDevice) }

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    ///end menu


    override fun onDestroy() {
        ConnectionManager.unregisterListener(connectionEventListener)
        ConnectionManager.teardownConnection(device)
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener)
        Log.w("ActivityState", "onDestroyBle")
        super.onDestroy()
    }

    //  override fun onRestart() {
    //    ConnectionManager.unregisterListener(connectionEventListener)
    //   ConnectionManager.teardownConnection(device)
    //     super.onRestart()
    // }


    override fun onBackPressed(): Unit {
        Intent(this, MainActivity::class.java).also {
            it.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            // it.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            ConnectionManager.unregisterListener(connectionEventListener)
            startActivity(it)
            overridePendingTransition(0, 0)
        }
        overridePendingTransition(0, 0)
    }


    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "Disconnected from device."
                        positiveButton("OK") { onBackPressed() }
                    }.show()
                }
            }
            onCharacteristicRead = { _, characteristic ->
                when (characteristic.uuid) {
                    FSERVICEFIRWARE -> {

                        val heartRate = characteristic.getStringValue(0)
                        Handler(Looper.getMainLooper()).post(Runnable {
                            hsrate.text = "Device firmware:  " + heartRate.toString()
                        })

                    }
                    BSERVICELEVEL -> {
                        val flag = characteristic.properties
                        val format = when (flag and 0x01) {
                            0x01 -> {
                                Log.d("TAG", "Battery level format UINT16.")
                                BluetoothGattCharacteristic.FORMAT_UINT16
                            }
                            else -> {
                                Log.d("TAG", "Battery level format UINT8.")
                                BluetoothGattCharacteristic.FORMAT_UINT8
                            }
                        }
                        val batteryRate = characteristic.getIntValue(format, 0)
                        Log.d("TAG", String.format("Recived battery level rate: %d", batteryRate))
                        btlvl.text = "Battery level  " + batteryRate.toString() + "%"

                    }
                }
            }
            onCharacteristicWrite = { _, characteristic ->

            }

            onMtuChanged = { _, mtu ->

            }

            onCharacteristicChanged = { _, characteristic ->
                when (characteristic.uuid) {
                    BSERVICELEVEL -> {
                        val flag = characteristic.properties
                        val format = when (flag and 0x01) {
                            0x01 -> {
                                Log.d("TAG", "Battery level format UINT16.")
                                BluetoothGattCharacteristic.FORMAT_UINT16
                            }
                            else -> {
                                Log.d("TAG", "Battery level format UINT8.")
                                BluetoothGattCharacteristic.FORMAT_UINT8
                            }
                        }
                        val batteryRate = characteristic.getIntValue(format, 0)
                        Log.d("TAG", String.format("Recived battery level rate: %d", batteryRate))
                        btlvl.text = "Battery level  " + batteryRate.toString()
                    }
                    number->{
                        val flag = characteristic.properties
                        val format = when (flag and 0x01) {
                            0x01 -> {
                                Log.d("TAG", "Number format UINT16.")
                                BluetoothGattCharacteristic.FORMAT_UINT16
                            }
                            else -> {
                                Log.d("TAG", "Number format UINT8.")
                                BluetoothGattCharacteristic.FORMAT_UINT8
                            }
                        }
                        val batteryRate = characteristic.getIntValue(format, 0)
                        Log.d("TAG", String.format("Number: %d", batteryRate))
                        //pathsomthing.text = "Number of " + batteryRate.toString()
                    }
                }
            }
            onNotificationsEnabled = { _, characteristic ->

                notifyingCharacteristics.add(characteristic.uuid)
            }

            onNotificationsDisabled = { _, characteristic ->

                notifyingCharacteristics.remove(characteristic.uuid)
            }
        }
    }

    private enum class CharacteristicProperty {
        Readable,
        Writable,
        WritableWithoutResponse,
        Notifiable,
        Indicatable;

        val action
            get() = when (this) {
                Readable -> "Read"
                Writable -> "Write"
                WritableWithoutResponse -> "Write Without Response"
                Notifiable -> "Toggle Notifications"
                Indicatable -> "Toggle Indications"
            }
    }

    private fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun EditText.showKeyboard() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        requestFocus()
        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()

    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")
        setContentView(R.layout.activity_ble_operations)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = getString(R.string.ble_playground)
        }
        overridePendingTransition(0, 0)
        devicename.text = device.name
        imeuredjaja.text = device.address
        //bottom menu
        setUpBottombar()
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener)
        //top menu
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                //      R.id.mItem -> Toast.makeText(applicationContext, "clicked", Toast.LENGTH_SHORT).show()
            }
            true
        }
       fmanager.setOnClickListener {  Intent(this@BleOperationsActivity, FileManager::class.java).also {
           it.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
           startActivity(it)
            }
       }
        //added
        device.connectGatt(this, false, callback)

        Log.w("ActivityState", "onCreateBle")
    }

    override fun onStart() {
        overridePendingTransition(0, 0)
        super.onStart()
        update_dfu()
        Log.w("ActivityState", "onStartBle")
    }

    override fun onRestart() {
        overridePendingTransition(0, 0)
        super.onRestart()
        Log.w("ActivityState", "onRestartBle")
    }

    override fun onResume() {
        overridePendingTransition(0, 0)
        setUpBottombar()
        btn_dc.setOnClickListener {
            ConnectionManager.teardownConnection(device)
            // conbledev?.remove(device)
            //    conDevAdapter.notifyDataSetChanged()

            Log.w("btn", "presed dc")
        }
        ConnectionManager.registerListener(connectionEventListener)
        Log.w("ActivityState", "onResumeBle")
        super.onResume()
    }

    override fun onPause() {
        overridePendingTransition(0, 0)
        ConnectionManager.unregisterListener(connectionEventListener)
        super.onPause()
        Log.w("ActivityState", "onPauseBle")
    }

    override fun onStop() {
        overridePendingTransition(0, 0)

        super.onStop()
        Log.w("ActivityState", "onStopBle")
    }


    //Adding dfu
    private fun update_dfu() {
        btnchose.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
            val selectedFile = intent.getParcelableExtra<Uri>("path")
        }
        otaStartButton.setOnClickListener {
            ConnectionManager.unregisterListener(connectionEventListener)

            val file = intent.getParcelableExtra<Uri>("path")
            if (file !== null) {
                val file1 = file.path
                pathsomthing.text = file.toString()
                val starter: DfuServiceInitiator = DfuServiceInitiator(device.address)
                    .setDeviceName(device.name)
                    .setKeepBond(false)
                    //       .setForceDfu(false)
                    .setZip(file)
                    .setPacketsReceiptNotificationsEnabled(true)
                    .setPacketsReceiptNotificationsValue(DfuServiceInitiator.DEFAULT_PRN_VALUE)
                    .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                    .setDisableNotification(false)

                // starter.start(this, DfuService::class.java)
                DfuServiceInitiator.createDfuNotificationChannel(this);
                val controller: DfuServiceController =
                    starter.start(this, DfuService::class.java)
                Log.w("start", "$starter")
                Log.w("files", "$file1")
                //
                // var dialog = ProgressDialog.progressDialog(this@BleOperationsActivity)
                // dialog.show()
                startProgress()
            }
        }
    }

    private var progressDialog: Dialog? = null
    fun startProgress() {
        val factory = LayoutInflater.from(this)
        val dialogView: View = factory.inflate(R.layout.custom_dialog, null)
        progressDialog = Dialog(this)
        progressDialog!!.setCancelable(false)
        progressDialog!!.setContentView(dialogView)
        progressDialog!!.show()

    }

    fun stopProgress() {
        progressDialog!!.dismiss()
    }

    //
    private val mDfuProgressListener = object : DfuProgressListenerAdapter() {
        override fun onDfuCompleted(deviceAddress: String) {
            toast("Update done")
            stopProgress()
            val intent = Intent(this@BleOperationsActivity, MainActivity::class.java)
            startActivity(intent)
        }

        override fun onError(
            deviceAddress: String,
            error: Int,
            errorType: Int,
            message: String?
        ) {
            toast("Error happened try again")
            stopProgress()
            val intent = Intent(this@BleOperationsActivity, MainActivity::class.java)
            startActivity(intent)

        }

        override fun onDfuProcessStarted(deviceAddress: String) {
            toast("Updating started")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {

            val path = data?.data
            intent.putExtra("path", path)
            setResult(Activity.RESULT_OK, intent)
            //   finish()

            Log.w("file1", "$$path")

        }
    }

    /////
    fun firstcommand(gatt: BluetoothGatt) {
        wbutt.setOnClickListener {
            //write characteristic
            val dd = UUID.fromString("adc0ca01-e16b-45c9-8980-3fe6300d5ded")
            //service
            val aa: UUID = UUID.fromString("adc0cab0-e16b-45c9-8980-3fe6300d5ded")

            val characteristic = gatt.getService(aa).getCharacteristic(dd)

            val bb = UUID.fromString("adc0ca02-e16b-45c9-8980-3fe6300d5ded")
            val cha3 = gatt.getService(aa).getCharacteristic(bb)

            val cc = UUID.fromString("adc0ca03-e16b-45c9-8980-3fe6300d5ded")
            val cha4 = gatt.getService(aa).getCharacteristic(cc)

            Log.i("*** CMD ", inputtext.text.toString())
            Log.i("*** PATH ", inputtext2.text.toString())

            var cmd = 0x00.toByte()
            when (inputtext.text.toString()) {
                "33" -> {
                    cmd = 0x33.toByte()
                }
                "99" -> {
                    cmd = 0x99.toByte()
                }
                "A1" -> {
                    cmd = 0xA1.toByte()
                }
                "A2" -> {
                    cmd = 0xA2.toByte()
                }
            }

            if (cmd != 0x00.toByte()) {
                val path = inputtext2.text.toString().toByteArray()
                val endCmd = 0x00.toByte()
                if (path.isEmpty()) {
                    Log.i("ble cmd", arrayOf(cmd, endCmd).toByteArray().toString())
                    writeCharacteristic(device, characteristic, arrayOf(cmd, endCmd).toByteArray())
                } else {
                    Log.i(
                        "ble cmd",
                        (arrayOf(cmd).toByteArray() + path + arrayOf(endCmd).toByteArray()).toString()
                    )
                    writeCharacteristic(
                        device,
                        characteristic,
                        arrayOf(cmd).toByteArray() + path + arrayOf(endCmd).toByteArray()
                    )
                }
            } else {
                val noCommand = 0x00.toByte()
                Log.i("ble cmd", arrayOf(noCommand).toByteArray().toString())
                writeCharacteristic(device, characteristic, arrayOf(noCommand).toByteArray())

            }
            //   inputtext.setText("")
            //   inputtext2.setText("")
        }
    }
    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                    firstcommand(gatt)
                }
            }
         else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.w("BluetoothGattCallback", "Successfully disconnected from ")
            gatt.close()
        }
     else {
        Log.w("BluetoothGattCallback", "Error $status encountered for ! Disconnecting...")
        gatt.close()
    }
        }
    }
}







