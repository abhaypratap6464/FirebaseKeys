package com.abhay.firebasekeys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapsApiKey = context.getString(R.string.google_maps_api_key)

    val delhi = LatLng(28.6139, 77.2090)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(delhi, 10f)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Key exposure proof panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color(0xFF212121), RoundedCornerShape(6.dp))
                .padding(10.dp)
        ) {
            Text(
                text = "Key used to load this map (from strings.xml):",
                fontSize = 11.sp, color = Color(0xFFAAAAAA), fontFamily = FontFamily.Monospace
            )
            Text(
                text = mapsApiKey,
                fontSize = 11.sp, color = Color(0xFFFF6B6B), fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$ apktool d app.apk && grep -r 'maps_api_key' app/res/",
                fontSize = 11.sp, color = Color(0xFF80FF80), fontFamily = FontFamily.Monospace
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                Marker(
                    state = MarkerState(position = delhi),
                    title = "Map loaded with stolen key!",
                    snippet = "Key extracted from strings.xml"
                )
            }

            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                color = Color(0xCCD32F2F),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Map loaded using exposed API key",
                    fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
