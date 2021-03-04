package com.example.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.row_connected_devices.*
import org.jetbrains.anko.toast

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)







        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.settings

    //    val menu=  bottomNavigationView.menu;
    //    val menuItem = menu.getItem(2)
    //    menuItem.isChecked = true

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.main -> {
                    Intent(this, MainActivity::class.java).also {
                        startActivity(it)
                    }
                    overridePendingTransition(0, 0)

                    return@setOnNavigationItemSelectedListener true

                }
                R.id.metrics -> {

                  toast("pressed")

                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.settings -> {

                    return@setOnNavigationItemSelectedListener false

                }
            }
            false
        }
    }
}
