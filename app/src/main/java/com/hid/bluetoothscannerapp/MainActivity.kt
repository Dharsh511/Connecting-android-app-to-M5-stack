package com.hid.bluetoothscannerapp

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hid.bluetoothscannerapp.blescanner.BleScanManager
import com.hid.bluetoothscannerapp.blescanner.adapter.BleDeviceAdapter
import com.hid.bluetoothscannerapp.blescanner.model.BleDevice
import com.hid.bluetoothscannerapp.blescanner.model.BleScanCallback

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Intent

@RequiresApi(Build.VERSION_CODES.S)
class MainActivity : AppCompatActivity() {
    private lateinit var btnStartScan: Button
    private lateinit var btManager: BluetoothManager
    private lateinit var bleScanManager: BleScanManager
    private lateinit var foundDevices: MutableList<BleDevice>
    private lateinit var context: Context

    private val REQUEST_CODE_BLUETOOTH_PERMISSION = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btManager = getSystemService(BluetoothManager::class.java)
        foundDevices = mutableListOf()

        val rvFoundDevices = findViewById<RecyclerView>(R.id.rv_found_devices)

        checkBluetoothPermission()
        val adapter = BleDeviceAdapter(foundDevices) { device ->
            connectToDevice(device)
        }
        rvFoundDevices.adapter = adapter
        rvFoundDevices.layoutManager = LinearLayoutManager(this)
        context = this

        btManager = getSystemService(BluetoothManager::class.java)
        bleScanManager = BleScanManager(btManager, 5000, scanCallback = BleScanCallback( {
            val name = it.name
            val rssi = it.rssi
            val address = it.address
            if (name.isBlank()) return@BleScanCallback

            val device = rssi.let { it1 -> BleDevice(name,address, it1) }
            if (!foundDevices.contains(device)) {
                Log.d(BleScanCallback::class.java.simpleName, "Found device: $name")

                foundDevices.add(device)
                adapter.notifyItemInserted(foundDevices.size - 1)
            }
        }))

        btnStartScan = findViewById(R.id.btn_start_scan)
        btnStartScan.setOnClickListener {
            if (checkBluetoothPermission()) {
                bleScanManager.scanBleDevices()
            } else {
                requestBluetoothPermission()
            }
        }

        if (checkBluetoothPermission()) {
            bleScanManager.scanBleDevices()
        } else {
            requestBluetoothPermission()
        }
    }


    private fun connectToDevice(device: BleDevice) {
        checkBluetoothPermission()
        // Notify the user that the connection process is starting
        Toast.makeText(this, "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()

        // Get the BluetoothAdapter instance
       //val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter


        // Get the BluetoothDevice object using the device address
        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)

        bluetoothDevice?.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to ${device.name} ${gatt.device.bondState}")
                  checkBluetoothPermission()

                    // After connection, you might want to discover services if needed
                    gatt.discoverServices()

                    // Move to DeviceControlActivity with the connected device details
                    val intent = Intent(this@MainActivity, DeviceInfoActivity::class.java).apply {
                        putExtra("DEVICE_NAME", device.name)
                        putExtra("DEVICE_ADDRESS", device.address)
                        putExtra("RSSI", device.rssi)
                    }
                    startActivity(intent)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("MainActivity", "Disconnected from ${device.name}")
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered on ${device.name}")
                    // Here you can interact with the services if needed
                } else {
                    Log.e(TAG, "Service discovery failed on ${device.name}, status: $status")
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Characteristic read successfully from ${device.name}")
                    // Handle characteristic read
                } else {
                    Log.e(TAG, "Characteristic read failed from ${device.name}, status: $status")
                }
            }

            // You can override more callback methods here as needed, such as onCharacteristicWrite, onDescriptorRead, etc.
        }) ?: run {
            // Handle the case where the BluetoothDevice is null
            Toast.makeText(this, "Device not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFoundDevices(adapter: BleDeviceAdapter) {
        val size = foundDevices.size
        foundDevices.clear()
        adapter.notifyItemRangeRemoved(0, size)
    }

    private fun checkBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermissions(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            hasPermissions(Manifest.permission.BLUETOOTH)
        }
    }

    private fun hasPermissions(vararg permissions: String): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestBluetoothPermission() {
        if (!checkBluetoothPermission()) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                arrayOf(Manifest.permission.BLUETOOTH)
            }
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_BLUETOOTH_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bleScanManager.scanBleDevices()
            } else {
                Toast.makeText(this, getString(R.string.ble_permissions_denied_message), Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
