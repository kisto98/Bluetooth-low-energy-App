package com.example.bluetooth

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.row_file_layout.view.*
import kotlinx.android.synthetic.main.row_folder_layout.view.*
import org.jetbrains.anko.layoutInflater

class FileAdapter(

    private val items: MutableList<String>?
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    var mSelectedItem = -1

    fun getSelectedItem(): Int {
        return mSelectedItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.context.layoutInflater.inflate(
            R.layout.row_file_layout,
            parent,
            false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items?.get(position)
        if (item != null) {
            holder.bind(item)
        }
        // holder.itemView.rButton.isChecked = position == mSelectedItem;
    }

    override fun getItemCount(): Int {
        if (items != null) {
            return items.size
        }
        return itemCount
    }


    inner class ViewHolder
    constructor(
        private val view: View
    ) : RecyclerView.ViewHolder(view) {

        fun bind(items: String?) {
            view.file_name.text = items.toString()
            view.setOnClickListener {
                mSelectedItem = adapterPosition
                notifyDataSetChanged()
            }

            //   onClickListener.invoke(bluetoothDevice)

        }
    }


}
