package com.hid.bluetoothscannerapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.UUID

class DeviceInfoActivity : AppCompatActivity() {

    private  var gatt: BluetoothGatt? =null
    private lateinit var deviceName: String
    private lateinit var deviceAddress: String
    private var rssi: Int = 0
    private lateinit var Uuid : String
    private lateinit var BondState: String
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_info)

        deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Unknown"
        deviceAddress = intent.getStringExtra("DEVICE_ADDRESS") ?: "Unknown"
        rssi = intent.getIntExtra("RSSI", 0)
      //  Uuid = intent.getStringExtra("UUID") ?: "-"
      //  BondState = intent.getStringExtra("BOND_STATE") ?: "-"

        val deviceInfoTextView = findViewById<TextView>(R.id.device_info)
        deviceInfoTextView.text = "Name: $deviceName\nAddress: $deviceAddress\nRSSI: $rssi\n"

        val unlockButton = findViewById<Button>(R.id.btn_unlock)
        val lockButton = findViewById<Button>(R.id.btn_lock)

        unlockButton.setOnClickListener {
            sendCommandToM5Stack("Unlock")
        }

        lockButton.setOnClickListener {
            sendCommandToM5Stack("Lock")
        }
    }


    private fun sendCommandToM5Stack(command: String) {
        // Assuming the M5Stack has a writable characteristic to send commands
        val service: BluetoothGattService? = gatt?.getService(UUID.fromString("YOUR_SERVICE_UUID"))
        val characteristic: BluetoothGattCharacteristic? = service?.getCharacteristic(UUID.fromString("YOUR_CHARACTERISTIC_UUID"))

        characteristic?.let {
            it.value = command.toByteArray()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            gatt?.writeCharacteristic(it)
            Toast.makeText(this, "Sent $command command", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Failed to send $command", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        gatt?.close()
    }
}


