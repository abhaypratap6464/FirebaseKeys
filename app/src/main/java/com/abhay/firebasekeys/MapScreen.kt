package com.abhay.firebasekeys

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// ── Billing rates (Google Maps Platform, 2025) ────────────────────────────────
private const val RATE_DYNAMIC_MAPS  = 0.007   // $7.00  per 1,000 map loads
private const val RATE_STATIC_MAPS   = 0.002   // $2.00  per 1,000 requests
private const val RATE_GEOCODING     = 0.005   // $5.00  per 1,000 requests
private const val FREE_CREDIT        = 200.0   // $200 free credit / month

// Cities cycled through on each Static Maps button tap
private val CITIES = listOf(
    "New Delhi"   to "28.6139,77.2090",
    "Mumbai"      to "19.0760,72.8777",
    "Bengaluru"   to "12.9716,77.5946",
    "Kolkata"     to "22.5726,88.3639",
    "Jaipur"      to "26.9124,75.7873",
    "Hyderabad"   to "17.3850,78.4867",
    "Chennai"     to "13.0827,80.2707",
    "Agra"        to "27.1767,78.0081",
)

// ── Tracker — mutableIntStateOf → Compose recomposes on every increment ───────
object ApiUsageTracker {
    var mapLoads       by mutableIntStateOf(0)
    var staticMapCalls by mutableIntStateOf(0)
    var geocodeCalls   by mutableIntStateOf(0)

    fun recordMapLoad()    { mapLoads++ }
    fun recordStaticMap()  { staticMapCalls++ }
    fun recordGeocode()    { geocodeCalls++ }
    fun reset()            { mapLoads = 0; staticMapCalls = 0; geocodeCalls = 0 }

    val mapLoadCost    get() = mapLoads       * RATE_DYNAMIC_MAPS
    val staticMapCost  get() = staticMapCalls * RATE_STATIC_MAPS
    val geocodeCost    get() = geocodeCalls   * RATE_GEOCODING
    val totalCost      get() = mapLoadCost + staticMapCost + geocodeCost
    val freeRemaining  get() = (FREE_CREDIT - totalCost).coerceAtLeast(0.0)
}

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    apiKey: String = try { NativeKeys.getGoogleApiKey() } catch (e: Throwable) { "DUMMY_KEY" }
) {
    val scope    = rememberCoroutineScope()
    val kb       = LocalSoftwareKeyboardController.current
    val http     = remember { OkHttpClient() }

    val delhi    = LatLng(28.6139, 77.2090)
    val camState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(delhi, 5f)
    }

    var query            by remember { mutableStateOf("") }
    var isSearching      by remember { mutableStateOf(false) }
    var searchError      by remember { mutableStateOf<String?>(null) }
    var searchResult     by remember { mutableStateOf<Pair<LatLng, String>?>(null) }

    var isLoadingStatic  by remember { mutableStateOf(false) }
    var staticMapBitmap  by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var staticMapLabel   by remember { mutableStateOf("") }
    var staticMapError   by remember { mutableStateOf<String?>(null) }
    var cityIndex        by remember { mutableIntStateOf(0) }
    // Incrementing this key forces GoogleMap to fully remount = 1 real Dynamic Map load
    var mapReloadCount   by remember { mutableIntStateOf(0) }

    // ── Static Maps API call ──────────────────────────────────────────────────
    fun loadStaticMap() {
        val (name, latlng) = CITIES[cityIndex % CITIES.size]
        cityIndex++
        scope.launch {
            isLoadingStatic = true
            staticMapBitmap = null
            staticMapError  = null
            try {
                val url = "https://maps.googleapis.com/maps/api/staticmap" +
                        "?center=$latlng&zoom=12&size=640x300&scale=2" +
                        "&maptype=roadmap" +
                        "&markers=color:red%7C$latlng" +
                        "&key=$apiKey"
                val (code, bytes) = withContext(Dispatchers.IO) {
                    val resp = http.newCall(Request.Builder().url(url).build()).execute()
                    resp.code to resp.body?.bytes()
                }
                if (code == 200 && bytes != null) {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        ApiUsageTracker.recordStaticMap()
                        staticMapBitmap = bmp
                        staticMapLabel  = name
                    } else {
                        // 200 but body wasn't a valid image (shouldn't happen)
                        staticMapError = "HTTP 200 but response is not a valid image"
                    }
                } else {
                    // Non-200 — body is a JSON/text error from Google
                    val errorBody = bytes?.let { String(it) }?.take(300) ?: "(no body)"
                    staticMapError = "HTTP $code — $errorBody"
                }
            } catch (e: Exception) {
                staticMapError = e.message ?: "Unknown error"
            } finally {
                isLoadingStatic = false
            }
        }
    }

    // ── Geocoding (search bar) ────────────────────────────────────────────────
    fun search(q: String) {
        if (q.isBlank()) return
        scope.launch {
            isSearching = true; searchError = null
            try {
                val url = "https://maps.googleapis.com/maps/api/geocode/json" +
                        "?address=${Uri.encode(q)}&key=$apiKey"
                val body = withContext(Dispatchers.IO) {
                    http.newCall(Request.Builder().url(url).build())
                        .execute().body?.string()
                } ?: return@launch
                val json   = JSONObject(body)
                ApiUsageTracker.recordGeocode()
                val status = json.getString("status")
                if (status == "OK") {
                    val r0  = json.getJSONArray("results").getJSONObject(0)
                    val loc = r0.getJSONObject("geometry").getJSONObject("location")
                    val ll  = LatLng(loc.getDouble("lat"), loc.getDouble("lng"))
                    searchResult = ll to r0.getString("formatted_address")
                    camState.animate(CameraUpdateFactory.newLatLngZoom(ll, 12f))
                } else {
                    searchError = "Not found ($status)"
                }
            } catch (e: Exception) {
                searchError = e.message
            } finally {
                isSearching = false
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Column: Controls & Static Map
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BillingDashboard(
                        apiKey          = apiKey,
                        isLoadingStatic = isLoadingStatic,
                        onSimulate      = ::loadStaticMap,
                        onReloadMap     = { mapReloadCount++ }
                    )
                    StaticMapResult(
                        isLoadingStatic = isLoadingStatic,
                        staticMapBitmap = staticMapBitmap,
                        staticMapLabel  = staticMapLabel,
                        error           = staticMapError
                    )
                }

                // Right Column: Search & Live Map
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchBar(
                        query = query,
                        onQueryChange = { query = it; searchError = null },
                        isSearching = isSearching,
                        searchError = searchError,
                        onSearch = { kb?.hide(); search(query) },
                        onClear = { query = ""; searchResult = null; searchError = null }
                    )

                    LiveMap(
                        camState     = camState,
                        searchResult = searchResult,
                        reloadCount  = mapReloadCount
                    )
                }
            }
        } else {
            // Narrow Screen: Vertical Scrollable List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BillingDashboard(
                    apiKey          = apiKey,
                    isLoadingStatic = isLoadingStatic,
                    onSimulate      = ::loadStaticMap,
                    onReloadMap     = { mapReloadCount++ }
                )

                StaticMapResult(
                    isLoadingStatic = isLoadingStatic,
                    staticMapBitmap = staticMapBitmap,
                    staticMapLabel  = staticMapLabel,
                    error           = staticMapError
                )

                SearchBar(
                    query = query,
                    onQueryChange = { query = it; searchError = null },
                    isSearching = isSearching,
                    searchError = searchError,
                    onSearch = { kb?.hide(); search(query) },
                    onClear = { query = ""; searchResult = null; searchError = null }
                )

                LiveMap(
                    modifier     = Modifier.height(300.dp),
                    camState     = camState,
                    searchResult = searchResult,
                    reloadCount  = mapReloadCount
                )
            }
        }
    }
}

@Composable
private fun StaticMapResult(
    isLoadingStatic: Boolean,
    staticMapBitmap: android.graphics.Bitmap?,
    staticMapLabel: String,
    error: String?
) {
    val boxMod = if (error != null)
        Modifier.fillMaxWidth()
    else
        Modifier.fillMaxWidth().height(200.dp)

    Box(
        modifier = boxMod
            .padding(horizontal = 8.dp)
            .background(
                if (error != null) Color(0xFFFFEBEE) else Color(0xFFEEEEEE),
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (error != null) 1.dp else 0.dp,
                color = if (error != null) Color(0xFFD32F2F) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingStatic -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Text("Calling Static Maps API…", fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp))
                }
            }

            error != null -> {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Static Maps API Error",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    Text(
                        text = error,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFB71C1C),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .background(Color(0xFFFFCDD2), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    )
                    Text(
                        "→ Enable 'Maps Static API' in Google Cloud Console for this key",
                        fontSize = 10.sp, color = Color(0xFF7F0000),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            staticMapBitmap != null -> {
                Image(
                    bitmap = staticMapBitmap.asImageBitmap(),
                    contentDescription = staticMapLabel,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = staticMapLabel,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color(0xDD000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = Color.White
                )
            }

            else -> {
                Text(
                    "Tap  \"Simulate Map API\"  to load a map image\nand consume the API key",
                    fontSize = 12.sp, color = Color.Gray,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    searchError: String?,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 13.sp),
                placeholder = { Text("Search a place…", fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                leadingIcon  = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = onClear) { 
                        Icon(Icons.Default.Close, "Clear") 
                    }
                },
                isError = searchError != null,
            )
            Button(
                onClick = onSearch,
                enabled = query.isNotBlank() && !isSearching,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (isSearching) CircularProgressIndicator(
                    Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White
                ) else Text("Go", fontSize = 13.sp)
            }
        }

        searchError?.let {
            Text("  $it", color = Color(0xFFD32F2F), fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
        }
    }
}

@Composable
private fun LiveMap(
    modifier: Modifier = Modifier.fillMaxSize(),
    camState: CameraPositionState,
    searchResult: Pair<LatLng, String>?,
    reloadCount: Int = 0
) {
    Box(modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        // key(reloadCount) tears down and recreates GoogleMap on every increment.
        // Each remount = 1 real Dynamic Maps SDK initialisation = 1 billable load.
        key(reloadCount) {
            LaunchedEffect(Unit) { ApiUsageTracker.recordMapLoad() }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camState
            ) {
                searchResult?.let { (pos, name) ->
                    Marker(state = MarkerState(pos), title = name, snippet = "Search result")
                }
            }
        }
    }
}

// ── Billing dashboard ─────────────────────────────────────────────────────────
@Composable
private fun BillingDashboard(
    apiKey: String,
    isLoadingStatic: Boolean,
    onSimulate: () -> Unit,
    onReloadMap: () -> Unit
) {
    val t = ApiUsageTracker

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .border(1.dp, Color(0xFFFF6F00), RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF8E1), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("API Billing Tracker",
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))

        // Two consume buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Dynamic Maps button
            Button(
                onClick = onReloadMap,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text("Load Dynamic Map", fontSize = 10.sp, maxLines = 1)
            }
            // Static Maps button
            Button(
                onClick = onSimulate,
                modifier = Modifier.weight(1f),
                enabled = !isLoadingStatic,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (isLoadingStatic) CircularProgressIndicator(
                    Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.White
                ) else Text("Load Static Map", fontSize = 10.sp, maxLines = 1)
            }
        }

        // Full API key
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text("API KEY (from NDK)",
                fontSize = 9.sp, color = Color(0xFFAAAAAA), fontFamily = FontFamily.Monospace)
            Text(
                text = apiKey,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF80FF80),
                maxLines = 2,
                overflow = TextOverflow.Visible
            )
        }

        HorizontalDivider(color = Color(0x33FF6F00))

        BillingRow(
            api   = "Maps SDK — Dynamic Maps",
            calls = t.mapLoads,
            rate  = RATE_DYNAMIC_MAPS,
            cost  = t.mapLoadCost,
            color = Color(0xFF1565C0)
        )

        HorizontalDivider(color = Color(0x33FF6F00))

        BillingRow(
            api   = "Static Maps API  ← button",
            calls = t.staticMapCalls,
            rate  = RATE_STATIC_MAPS,
            cost  = t.staticMapCost,
            color = Color(0xFF2E7D32)
        )

        HorizontalDivider(color = Color(0x33FF6F00))

        BillingRow(
            api   = "Geocoding API  ← search",
            calls = t.geocodeCalls,
            rate  = RATE_GEOCODING,
            cost  = t.geocodeCost,
            color = Color(0xFF6A1B9A)
        )

        HorizontalDivider(color = Color(0xFFFF6F00).copy(alpha = 0.4f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Session total", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                formatCost(t.totalCost),
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (t.totalCost > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
            )
        }
        Text(
            "Free credit remaining ≈ ${formatCost(t.freeRemaining)} / \$200.00 /mo",
            fontSize = 10.sp, color = Color(0xFF795548)
        )
    }
}

@Composable
private fun BillingRow(api: String, calls: Int, rate: Double, cost: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(api, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
            Text("\$${"%.3f".format(rate * 1000)}/1k calls",
                fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$calls call${if (calls != 1) "s" else ""}",
                fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
            Text(
                formatCost(cost),
                fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = if (cost > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
            )
        }
    }
}

private fun formatCost(cost: Double) = "\$${"%,.4f".format(cost)}"

@Preview(showBackground = true, widthDp = 400)
@Composable
fun MapScreenPreviewPortrait() {
    MapScreen(apiKey = "AIzaSy_PREVIEW_ONLY_PORTRAIT")
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun MapScreenPreviewLandscape() {
    MapScreen(apiKey = "AIzaSy_PREVIEW_ONLY_LANDSCAPE")
}
