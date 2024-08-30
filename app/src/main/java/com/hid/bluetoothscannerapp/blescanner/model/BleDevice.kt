package com.hid.bluetoothscannerapp.blescanner.model

data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int
)
