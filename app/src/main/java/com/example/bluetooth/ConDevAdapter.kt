package com.example.bluetooth

import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.*
import androidx.recyclerview.widget.RecyclerView
import com.punchthrough.blestarterappandroid.ScanResultAdapter
import kotlinx.android.synthetic.main.row_connected_devices.view.*
import org.jetbrains.anko.bluetoothManager
import org.jetbrains.anko.layoutInflater



class ConDevAdapter(

        private val items: MutableList<BluetoothDevice>?,
        private val onClickListener: ((device: BluetoothDevice) -> Unit)

) : RecyclerView.Adapter<ConDevAdapter.ViewHolder>(){


    private var mSelectedItem = -1
    fun getSelectedItem():Int{
        return  mSelectedItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.context.layoutInflater.inflate(
                R.layout.row_connected_devices,
                parent,
                false
        )
        return ViewHolder(view, onClickListener)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items?.get(position)
        if (item != null) {
            holder.bind(item)
        }
        holder.itemView.radioButton.setChecked(position == mSelectedItem);
    }

    override fun getItemCount(): Int {
        if (items != null) {
            return items.size
        }
        return itemCount
    }


    inner class ViewHolder
    constructor(
            private val view: View,
            private val onClickListener: ((device: BluetoothDevice) -> Unit)
    ): RecyclerView.ViewHolder(view){

        fun bind(bluetoothDevice: BluetoothDevice){
            view.con_device_name.text = bluetoothDevice.name
            view.con_mac_address.text= bluetoothDevice.address
            view.btn_disconnect.setOnClickListener { ConnectionManager.teardownConnection(bluetoothDevice)  }
            view.setOnClickListener { onClickListener.invoke(bluetoothDevice) }

            itemView.radioButton.setOnClickListener {
                mSelectedItem=getAdapterPosition()
                notifyDataSetChanged();
            }

        }

    }


}







//lateinit var device: BluetoothDevice

