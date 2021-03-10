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
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetooth.ConnectionManager.UUID_HEART_RATE_MEASUREMENT
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_ble_operations.*
import kotlinx.android.synthetic.main.nav_header.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.bluetoothManager
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
        Log.w("selected", "s$bottomNavigationView.selectedItemId")
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

    private val conbledev: MutableList<BluetoothDevice>? by lazy { bluetoothManager.getConnectedDevices(BluetoothProfile.GATT) }
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

            }

            onCharacteristicWrite = { _, characteristic ->

            }

            onMtuChanged = { _, mtu ->

            }

            onCharacteristicChanged = { _, characteristic ->
                ////my add

                when (characteristic.uuid) {
                    ConnectionManager.BATTERY_SERVICE -> {
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
                    UUID_HEART_RATE_MEASUREMENT -> {
                        val flag = characteristic.properties
                        Log.d("TAG", "Heart rate flag: $flag")
                        var format = -1
                        // Heart rate bit number format
                        if (flag and 0x01 != 0) {
                            format = BluetoothGattCharacteristic.FORMAT_UINT16
                            Log.d("TAG", "Heart rate format UINT16.")

                        } else {
                            format = BluetoothGattCharacteristic.FORMAT_UINT8
                            Log.d("TAG", "Heart rate format UINT8.")
                        }
                        val heartRate = characteristic.getIntValue(format, 1)
                        Log.w("TAG", String.format("Received heart rate: %d", heartRate))
                        heartRate.toDouble()
                        hsrate.text = "Heart rate sensor  " + heartRate.toString()
                        devicename.text = "Device name: " + device.name
                        ///
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
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun EditText.showKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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

        Log.w("ActivityState", "onCreateBle")
    }

    override fun onStart() {
        overridePendingTransition(0, 0)
        super.onStart()
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
        super.onPause()
        Log.w("ActivityState", "onPauseBle")
    }
    override fun onStop() {
        overridePendingTransition(0, 0)
        super.onStop()
        Log.w("ActivityState", "onStopBle")
    }



}
////


//var characteristic: BluetoothGattCharacteristic = bluetoothGatt.getService(HEART_RATE_SERVICE_UUID).getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID)
//lateinit var bluetoothGatt: BluetoothGatt


