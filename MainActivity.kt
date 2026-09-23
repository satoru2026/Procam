package com.procam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.procam.app.ui.CameraScreen
import com.procam.app.ui.GalleryScreen
import com.procam.app.ui.SettingsScreen
import com.procam.app.ui.theme.ProCamTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProCamTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val permissionsState = rememberMultiplePermissionsState(
                        listOf(
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.RECORD_AUDIO
                        )
                    )
                    if (permissionsState.allPermissionsGranted) {
                        ProCamNavHost(viewModel)
                    } else {
                        PermissionRequestScreen(permissionsState)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRequestScreen(permissionsState: MultiplePermissionsState) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column {
            Text("ProCam memerlukan izin kamera & mikrofon untuk berfungsi.")
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Berikan izin")
            }
        }
    }
}

@Composable
private fun ProCamNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "camera") {
        composable("camera") {
            CameraScreen(
                viewModel = viewModel,
                onOpenGallery = { navController.navigate("gallery") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("gallery") {
            GalleryScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

// small helper alias so Column is available without extra import churn above
private typealias Column = androidx.compose.foundation.layout.Column
