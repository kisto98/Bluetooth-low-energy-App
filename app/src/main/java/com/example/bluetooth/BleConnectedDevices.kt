package com.example.bluetooth

import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetooth.ConnectionManager.isConnected
import kotlinx.android.synthetic.main.row_characteristic.view.*
import kotlinx.android.synthetic.main.row_scan_result.view.*
import org.jetbrains.anko.layoutInflater
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class BleConnectedDevicesAdapter(private val Connlist: List<BleConnectedDevice>):RecyclerView.Adapter<BleConnectedDevicesAdapter.BleConnectedDevice>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BleConnectedDevice {
        val itemView=LayoutInflater.from(parent.context).inflate(R.layout.row_connected, parent, false)
        return BleConnectedDevice(itemView)
    }

    override fun getItemCount() = Connlist.size

    override fun onBindViewHolder(holder: BleConnectedDevice, position: Int) {

    }

    class BleConnectedDevice(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val textView= itemView.mac_address
        val textView2= itemView.signal_strength
        val textView3= itemView.device_name


        fun connected(device: BluetoothDevice, context: Context) {
            if (device.isConnected()) {


            }
        }
    }
    private lateinit var device: BluetoothDevice

}
