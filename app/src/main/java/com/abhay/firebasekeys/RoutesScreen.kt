package com.abhay.firebasekeys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun RoutesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val apiKey = context.getString(R.string.google_api_key)
    val tabs = listOf("Directions", "Distance Matrix", "Snap to Roads", "Route Optimization")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp) }
                )
            }
        }
        when (selectedTab) {
            0 -> DirectionsTab(apiKey)
            1 -> DistanceMatrixTab(apiKey)
            2 -> SnapToRoadsTab(apiKey)
            3 -> RouteOptimizationTab(apiKey)
        }
    }
}

@Composable
fun DirectionsTab(apiKey: String) {
    val scope = rememberCoroutineScope()
    var origin by remember { mutableStateOf("New Delhi, India") }
    var destination by remember { mutableStateOf("Mumbai, India") }
    var travelMode by remember { mutableStateOf("DRIVE") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val modes = listOf("DRIVE", "WALK", "TRANSIT", "BICYCLE")

    ApiTabLayout(
        title = "Routes API — Directions & ETA",
        description = "POST https://routes.googleapis.com/directions/v2:computeRoutes",
        onCall = {
            scope.launch {
                isLoading = true
                result = callRoutesApi(apiKey, origin, destination, travelMode)
                isLoading = false
            }
        },
        isLoading = isLoading,
        result = result
    ) {
        OutlinedTextField(value = origin, onValueChange = { origin = it },
            label = { Text("Origin") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = destination, onValueChange = { destination = it },
            label = { Text("Destination") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        Text("Travel Mode:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            modes.forEach { mode ->
                FilterChip(
                    selected = travelMode == mode,
                    onClick = { travelMode = mode },
                    label = { Text(mode, fontSize = 11.sp) }
                )
            }
        }
    }
}

@Composable
fun DistanceMatrixTab(apiKey: String) {
    val scope = rememberCoroutineScope()
    var origins by remember { mutableStateOf("New Delhi, India\nJaipur, India") }
    var destinations by remember { mutableStateOf("Mumbai, India\nBangalore, India") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    ApiTabLayout(
        title = "Routes API — Distance Matrix",
        description = "POST https://routes.googleapis.com/distanceMatrix/v2:computeRouteMatrix",
        onCall = {
            scope.launch {
                isLoading = true
                result = callDistanceMatrixApi(apiKey, origins, destinations)
                isLoading = false
            }
        },
        isLoading = isLoading,
        result = result
    ) {
        OutlinedTextField(value = origins, onValueChange = { origins = it },
            label = { Text("Origins (one per line)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = destinations, onValueChange = { destinations = it },
            label = { Text("Destinations (one per line)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    }
}

@Composable
fun SnapToRoadsTab(apiKey: String) {
    val scope = rememberCoroutineScope()
    var path by remember { mutableStateOf("28.6139,77.2090|28.6200,77.2150|28.6250,77.2200") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    ApiTabLayout(
        title = "Roads API — Snap to Roads",
        description = "GET https://roads.googleapis.com/v1/snapToRoads",
        onCall = {
            scope.launch {
                isLoading = true
                result = callSnapToRoadsApi(apiKey, path)
                isLoading = false
            }
        },
        isLoading = isLoading,
        result = result
    ) {
        OutlinedTextField(value = path, onValueChange = { path = it },
            label = { Text("GPS Points (lat,lng|lat,lng|...)") },
            modifier = Modifier.fillMaxWidth(), minLines = 2)
        Spacer(Modifier.height(4.dp))
        Text("Enter GPS coordinates to snap to the nearest roads.",
            fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun RouteOptimizationTab(apiKey: String) {
    val scope = rememberCoroutineScope()
    var depot by remember { mutableStateOf("28.6139,77.2090") }
    var stops by remember { mutableStateOf("28.6200,77.2150\n28.5500,77.1800\n28.7000,77.1000") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    ApiTabLayout(
        title = "Route Optimization API",
        description = "POST https://routeoptimization.googleapis.com/v1/projects/-:optimizeTours",
        onCall = {
            scope.launch {
                isLoading = true
                result = callRouteOptimizationApi(apiKey, depot, stops)
                isLoading = false
            }
        },
        isLoading = isLoading,
        result = result
    ) {
        OutlinedTextField(value = depot, onValueChange = { depot = it },
            label = { Text("Depot (lat,lng)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = stops, onValueChange = { stops = it },
            label = { Text("Delivery Stops (one lat,lng per line)") },
            modifier = Modifier.fillMaxWidth(), minLines = 3)
    }
}

@Composable
fun ApiTabLayout(
    title: String,
    description: String,
    onCall: () -> Unit,
    isLoading: Boolean,
    result: String,
    inputs: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(description, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { inputs() }
        Button(
            onClick = onCall,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isLoading) "Calling API..." else "Call API")
        }
        if (result.isNotEmpty()) {
            Text("Response:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                text = result,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A2E), RoundedCornerShape(6.dp))
                    .padding(10.dp),
                color = Color(0xFF80FF80)
            )
        }
    }
}

// ── API Calls ──────────────────────────────────────────────────────────────────

private suspend fun callRoutesApi(apiKey: String, origin: String, destination: String, mode: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("origin", JSONObject().apply {
                    put("address", origin)
                })
                put("destination", JSONObject().apply {
                    put("address", destination)
                })
                put("travelMode", mode)
                put("computeAlternativeRoutes", false)
                put("routeModifiers", JSONObject().apply { put("avoidTolls", false) })
                put("languageCode", "en-US")
                put("units", "METRIC")
            }.toString()

            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://routes.googleapis.com/directions/v2:computeRoutes")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.legs.steps.navigationInstruction")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "(empty)"
            val parsed = runCatching { JSONObject(responseBody) }.getOrNull()
            parsed?.let { formatRoutesResponse(it) } ?: responseBody
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

private fun formatRoutesResponse(json: JSONObject): String {
    val routes = json.optJSONArray("routes") ?: return "No routes found\n\n${json}"
    if (routes.length() == 0) return "No routes found\n\n${json}"
    val route = routes.getJSONObject(0)
    val distanceM = route.optInt("distanceMeters")
    val duration = route.optString("duration")
    val sb = StringBuilder()
    sb.appendLine("Distance: ${distanceM / 1000.0} km")
    sb.appendLine("Duration: $duration")
    sb.appendLine()
    val legs = route.optJSONArray("legs")
    if (legs != null && legs.length() > 0) {
        val steps = legs.getJSONObject(0).optJSONArray("steps")
        if (steps != null) {
            sb.appendLine("Steps:")
            for (i in 0 until minOf(steps.length(), 5)) {
                val step = steps.getJSONObject(i)
                val instruction = step.optJSONObject("navigationInstruction")?.optString("instructions") ?: ""
                if (instruction.isNotEmpty()) sb.appendLine("  ${i + 1}. $instruction")
            }
            if (steps.length() > 5) sb.appendLine("  ... and ${steps.length() - 5} more steps")
        }
    }
    return sb.toString()
}

private suspend fun callDistanceMatrixApi(apiKey: String, origins: String, destinations: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val originList = origins.lines().filter { it.isNotBlank() }
            val destList = destinations.lines().filter { it.isNotBlank() }

            val body = JSONObject().apply {
                put("origins", JSONArray().apply {
                    originList.forEach { put(JSONObject().apply { put("address", it.trim()) }) }
                })
                put("destinations", JSONArray().apply {
                    destList.forEach { put(JSONObject().apply { put("address", it.trim()) }) }
                })
                put("travelMode", "DRIVE")
                put("units", "METRIC")
            }.toString()

            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://routes.googleapis.com/distanceMatrix/v2:computeRouteMatrix")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Goog-FieldMask", "originIndex,destinationIndex,duration,distanceMeters,status")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "(empty)"
            val parsed = runCatching { JSONArray(responseBody) }.getOrNull()
            if (parsed != null) {
                val sb = StringBuilder()
                sb.appendLine("Matrix (${originList.size} origins × ${destList.size} destinations):\n")
                for (i in 0 until parsed.length()) {
                    val element = parsed.getJSONObject(i)
                    val oIdx = element.optInt("originIndex")
                    val dIdx = element.optInt("destinationIndex")
                    val dist = element.optInt("distanceMeters")
                    val dur = element.optString("duration")
                    sb.appendLine("${originList.getOrElse(oIdx) { "O$oIdx" }.trim()} → " +
                            "${destList.getOrElse(dIdx) { "D$dIdx" }.trim()}")
                    sb.appendLine("  Distance: ${dist / 1000.0} km  |  Duration: $dur\n")
                }
                sb.toString()
            } else responseBody
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

private suspend fun callSnapToRoadsApi(apiKey: String, path: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(path.trim(), "UTF-8")
            val url = "https://roads.googleapis.com/v1/snapToRoads?path=$encoded&interpolate=true&key=$apiKey"
            val client = OkHttpClient()
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val responseBody = response.body?.string() ?: "(empty)"
            val parsed = runCatching { JSONObject(responseBody) }.getOrNull()
            if (parsed != null) {
                val points = parsed.optJSONArray("snappedPoints")
                if (points != null) {
                    val sb = StringBuilder()
                    sb.appendLine("Snapped ${points.length()} points to roads:\n")
                    for (i in 0 until points.length()) {
                        val pt = points.getJSONObject(i)
                        val loc = pt.optJSONObject("location")
                        val lat = loc?.optDouble("latitude") ?: 0.0
                        val lng = loc?.optDouble("longitude") ?: 0.0
                        val placeId = pt.optString("placeId", "")
                        sb.appendLine("Point $i: $lat, $lng")
                        if (placeId.isNotEmpty()) sb.appendLine("  Road PlaceId: $placeId")
                    }
                    sb.toString()
                } else "No snapped points\n\n$responseBody"
            } else responseBody
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

private suspend fun callRouteOptimizationApi(apiKey: String, depot: String, stops: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val depotParts = depot.split(",")
            val depotLat = depotParts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: 28.6139
            val depotLng = depotParts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 77.2090

            val stopLines = stops.lines().filter { it.isNotBlank() }

            val shipments = JSONArray()
            stopLines.forEachIndexed { index, stop ->
                val parts = stop.split(",")
                val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return@forEachIndexed
                val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return@forEachIndexed
                shipments.put(JSONObject().apply {
                    put("deliveries", JSONArray().apply {
                        put(JSONObject().apply {
                            put("arrivalLocation", JSONObject().apply {
                                put("latitude", lat)
                                put("longitude", lng)
                            })
                            put("duration", "120s")
                        })
                    })
                    put("label", "Stop ${index + 1}")
                })
            }

            val body = JSONObject().apply {
                put("model", JSONObject().apply {
                    put("shipments", shipments)
                    put("vehicles", JSONArray().apply {
                        put(JSONObject().apply {
                            put("startLocation", JSONObject().apply {
                                put("latitude", depotLat)
                                put("longitude", depotLng)
                            })
                            put("endLocation", JSONObject().apply {
                                put("latitude", depotLat)
                                put("longitude", depotLng)
                            })
                            put("label", "Vehicle 1")
                        })
                    })
                })
            }.toString()

            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://routeoptimization.googleapis.com/v1/projects/-:optimizeTours")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", apiKey)
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "(empty)"
            val parsed = runCatching { JSONObject(responseBody) }.getOrNull()
            if (parsed != null) {
                val routes = parsed.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val visits = route.optJSONArray("visits")
                    val sb = StringBuilder()
                    sb.appendLine("Optimized route for ${stopLines.size} stops:\n")
                    if (visits != null) {
                        for (i in 0 until visits.length()) {
                            val visit = visits.getJSONObject(i)
                            val shipmentIdx = visit.optInt("shipmentIndex")
                            val startTime = visit.optString("startTime", "")
                            sb.appendLine("Visit ${i + 1}: Stop ${shipmentIdx + 1}  |  Start: $startTime")
                        }
                    }
                    val metrics = parsed.optJSONObject("metrics")
                    if (metrics != null) {
                        sb.appendLine("\nTotal distance: ${metrics.optJSONObject("aggregatedRouteMetrics")?.optInt("travelDistanceMeters")?.let { "${it / 1000.0} km" } ?: "N/A"}")
                        sb.appendLine("Total duration: ${metrics.optJSONObject("aggregatedRouteMetrics")?.optString("travelDuration") ?: "N/A"}")
                    }
                    sb.toString()
                } else responseBody
            } else responseBody
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
