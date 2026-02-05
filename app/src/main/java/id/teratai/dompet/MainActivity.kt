package id.teratai.dompet

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.teratai.dompet.scan.ReceiptScannerScreen

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

                Scaffold { padding ->
                    if (!hasCameraPermission) {
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
                            ReceiptScannerScreen()
                        }
                    }
                }
            }
        }
    }
}
