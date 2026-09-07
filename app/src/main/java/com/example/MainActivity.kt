package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.data.util.DeviceGpsHelper
import com.example.ui.MaritimeApp
import com.example.ui.viewmodel.MaritimeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MaritimeViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.syncWithRealDeviceGps(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        if (DeviceGpsHelper.hasLocationPermission(this)) {
            viewModel.syncWithRealDeviceGps(this)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        setContent {
            MaritimeApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        if (DeviceGpsHelper.hasLocationPermission(this)) {
            viewModel.syncWithRealDeviceGps(this)
        }
    }
}

