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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Dompet Teratai — Receipt OCR", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Status kamera: " + if (hasCameraPermission) "OK" else "Belum diizinkan",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Minta izin kamera")
                        }

                        Text(
                            "Next milestone: CameraX preview + capture, lalu OCR ML Kit dan tampilkan hasil teks.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
