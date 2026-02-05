package id.teratai.dompet

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.teratai.dompet.history.HistoryScreen
import id.teratai.dompet.history.TransactionDetailScreen
import id.teratai.dompet.scan.ReceiptScannerScreen

private enum class Route { Scan, History, Detail }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var hasCameraPermission by remember { mutableStateOf(false) }
                val permissionLauncher = remember {
                    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                        hasCameraPermission = granted
                    }
                }

                var route by rememberSaveable { mutableStateOf(Route.Scan) }
                var selectedId by rememberSaveable { mutableLongStateOf(0L) }

                BackHandler(enabled = route == Route.Detail) {
                    route = Route.History
                }

                Scaffold(
                    bottomBar = {
                        if (route != Route.Detail) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = route == Route.Scan,
                                    onClick = { route = Route.Scan },
                                    label = { Text("Scan") },
                                    icon = { }
                                )
                                NavigationBarItem(
                                    selected = route == Route.History,
                                    onClick = { route = Route.History },
                                    label = { Text("History") },
                                    icon = { }
                                )
                            }
                        }
                    }
                ) { padding ->
                    if (route == Route.Scan && !hasCameraPermission) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Dompet Teratai — Receipt OCR", style = MaterialTheme.typography.titleLarge)
                            Text("Izin kamera dibutuhkan untuk scan struk.")
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text("Minta izin kamera")
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            when (route) {
                                Route.Scan -> ReceiptScannerScreen()
                                Route.History -> HistoryScreen(onOpen = { id ->
                                    selectedId = id
                                    route = Route.Detail
                                })
                                Route.Detail -> TransactionDetailScreen(
                                    id = selectedId,
                                    onBack = { route = Route.History }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
