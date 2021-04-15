package com.example.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import com.chootdev.recycleclick.RecycleClick
import com.example.bluetooth.ConnectionManager.writeCharacteristic
import kotlinx.android.synthetic.main.activity_file_manager2.*
import kotlinx.android.synthetic.main.row_file_layout.view.*
import kotlinx.android.synthetic.main.row_folder_layout.*
import kotlinx.android.synthetic.main.row_folder_layout.view.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileOutputStream
import java.util.*


class FileManager : AppCompatActivity() {

    lateinit var device: BluetoothDevice
    private var notifyingCharacteristics = mutableListOf<UUID>()
    //write characteristic
    val dd = UUID.fromString("adc0ca01-e16b-45c9-8980-3fe6300d5ded")
    //service
    val aa: UUID = UUID.fromString("adc0cab0-e16b-45c9-8980-3fe6300d5ded")
    //bytarray
    val tritri = 0x33.toByte()
    val nulanula = 0x00.toByte()
    val ajedan = 0xA1.toByte()
    val adva = 0xA2.toByte()
    val ANSWER = UUID.fromString("adc0ca02-e16b-45c9-8980-3fe6300d5ded")

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
                when (characteristic.uuid) {
                    ANSWER -> {

                        val ff = characteristic.getStringValue(0)
                        Log.d("TAG", "Recived FF $ff")

                        if (ff.contains("<d>")) {
                           val name= ff.replace("<d>", "").replace("</d>", "")
                            if (name !in folder) {
                                folder.add(0, name)
                                Handler(Looper.getMainLooper()).post(Runnable { folderAdapter.notifyDataSetChanged() })
                            }
                          //  fileAdapter.notifyDataSetChanged()
                        }
                        else if (ff.contains("<f>")) {
                            val name= ff.replace("<f>", "").replace("</f>", "")
                            if (name !in file) {
                                file.add(0, name)
                                Handler(Looper.getMainLooper()).post(Runnable { fileAdapter.notifyDataSetChanged() })
                            }
                           // folderAdapter.notifyDataSetChanged()
                        }

                        else {

                        }
                        Log.d("folderarray", "$folder")
                        Log.d("filearray", "$file")

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


    private fun setupFolderRecyclerView() {
        foldermanager.apply {
            foldermanager?.layoutManager = LinearLayoutManager(this@FileManager)
            adapter = folderAdapter
        }
    }

    private val folder = mutableListOf<String>()
    private val folderAdapter: FolderAdapter by lazy {
        FolderAdapter(folder)
    }

    //
    private fun setupFileRecyclerView() {
        filemanager.apply {
            filemanager?.layoutManager = LinearLayoutManager(this@FileManager)
            adapter = fileAdapter
        }
    }

    private val file = mutableListOf<String>()
    private val fileAdapter: FileAdapter by lazy {
        FileAdapter(file)
    }

    private val callback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("tag", "Connected to GATT server in filemanager.");
                    gatt.discoverServices()
                    Log.i("tag", "Attempting to start service discovery:" + gatt.discoverServices())
                        onclickcommands(gatt)
                }
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val characteristic = gatt.getService(aa).getCharacteristic(dd)
            writeCharacteristic(
                    device, characteristic,
                    arrayOf(tritri, nulanula).toByteArray()
                )
            writeCharacteristic(
                    device, characteristic,
                    arrayOf(ajedan, nulanula).toByteArray()
                )
        }
    }

    fun onclickcommands(gatt: BluetoothGatt) {

        RecycleClick.addTo(foldermanager)
            .setOnItemClickListener { recyclerView, position, v ->
                val characteristic = gatt.getService(aa).getCharacteristic(dd)
                folder.clear()
                file.clear()
                folderAdapter.notifyDataSetChanged();
                fileAdapter.notifyDataSetChanged();
                val path = v.folder_name.text.toString().toByteArray()
                writeCharacteristic(
                    device, characteristic,
                    arrayOf(ajedan).toByteArray() + path + arrayOf(nulanula).toByteArray()
                )
                Log.d("btn", "pressed")
            }

        RecycleClick.addTo(filemanager)
            .setOnItemClickListener { recyclerView, position, v ->
                val characteristic = gatt.getService(aa).getCharacteristic(dd)
            //    folder.clear()
            //    file.clear()
            //    folderAdapter.notifyDataSetChanged();
            //    fileAdapter.notifyDataSetChanged();
                val path = v.file_name.text.toString().toByteArray()
                writeCharacteristic(
                    device, characteristic,
                    arrayOf(adva).toByteArray() + path + arrayOf(nulanula).toByteArray()
                )
                Log.d("download", "pressed")
            }

        refresh.setOnClickListener {
            val characteristic = gatt.getService(aa).getCharacteristic(dd)
            folder.clear()
            file.clear()
            folderAdapter.notifyDataSetChanged();
            fileAdapter.notifyDataSetChanged();
            writeCharacteristic(
                device, characteristic,
                arrayOf(ajedan).toByteArray() +  arrayOf(nulanula).toByteArray()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from BleOperationActivity!")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_manager2)
        ConnectionManager.registerListener(connectionEventListener)
        device.connectGatt(this@FileManager,false, callback)
        setupFolderRecyclerView()
        setupFileRecyclerView()
    }
}