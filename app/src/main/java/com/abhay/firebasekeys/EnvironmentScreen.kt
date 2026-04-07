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
import org.json.JSONObject

@Composable
fun EnvironmentScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val apiKey = context.getString(R.string.google_api_key)
    val tabs = listOf("Air Quality", "Pollen", "Solar", "Weather")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 13.sp) }
                )
            }
        }
        when (selectedTab) {
            0 -> AirQualityTab(apiKey)
            1 -> PollenTab(apiKey)
            2 -> SolarTab(apiKey)
            3 -> WeatherTab(apiKey)
        }
    }
}

@Composable
fun AirQualityTab(apiKey: String) {
    val scope = rememberCoroutineScope()
    var lat by remember { mutableStateOf("28.6139") }
    var lng by remember { mutableStateOf("77.2090") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    EnvTabLayout(
        title = "Air Quality API",
        description = "POST https://airquality.googleapis.com/v1/currentConditions:lookup",
        onCall = {
            scope.launch {
                isLoading = true
                result = callAirQualityApi(apiKey, lat.toDoubleOrNull() ?: 28.6139, lng.toDoubleOrNull() ?: 77.2090)
                isLoading = false
            }
        },
        isLoading = isLoading,
        result = result
    ) {
        LocationInputs(lat, lng, onLatChange = { lat = it }, onLngChange = { lng = it })
    }
}

@Composable
fun PollenTab(apiKey: String) {
    val scope = rememberCoroutineScope()
    var lat by remember { mutableStateOf("28.6139") }
    var lng by remember { mutableStateOf("77.2090") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    EnvTabLayout(
        title = "Pollen API",
        description = "GET https://pollen.googleapis.com/v1/forecast:lookup",
        onCall = {
            scope.launch {
                isLoading = true
                result = callPollenApi(apiKey, lat.toDoubleOrNull() ?: 28.6139, lng.toDoubleOrNull() ?: 77.2090)
                isLoading = false
            }
        },
        isLoading = isLoading,
        result = result
    ) {
        LocationInputs(lat, lng, onLatChange = { lat = it }, onLngChange = { lng = it })
    }
}

@Composable
fun SolarTab(apiKey: String) {
    val scope = rememberCoroutineScope()
    var lat by remember { mutableStateOf("28.6139") }
    var lng by remember { mutableStateOf("77.2090") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    EnvTabLayout(
        title = "Solar API — Rooftop Insights",
        description = "GET https://solar.googleapis.com/v1/buildingInsights:findClosest",
        onCall = {
            scope.launch {
                isLoading = true
                result = callSolarApi(apiKey, lat.toDoubleOrNull() ?: 28.6139, lng.toDoubleOrNull() ?: 77.2090)
                isLoading = false
            }
        },
        isLoading = isLoading,
        result = result
    ) {
        LocationInputs(lat, lng, onLatChange = { lat = it }, onLngChange = { lng = it })
        Spacer(Modifier.height(4.dp))
        Text("Returns solar potential, panel count, and yearly energy for this rooftop.",
            fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun WeatherTab(apiKey: String) {
    val scope = rememberCoroutineScope()
    var lat by remember { mutableStateOf("28.6139") }
    var lng by remember { mutableStateOf("77.2090") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    EnvTabLayout(
        title = "Weather API",
        description = "GET https://weather.googleapis.com/v1/forecast/hours:lookup",
        onCall = {
            scope.launch {
                isLoading = true
                result = callWeatherApi(apiKey, lat.toDoubleOrNull() ?: 28.6139, lng.toDoubleOrNull() ?: 77.2090)
                isLoading = false
            }
        },
        isLoading = isLoading,
        result = result
    ) {
        LocationInputs(lat, lng, onLatChange = { lat = it }, onLngChange = { lng = it })
    }
}

@Composable
fun LocationInputs(
    lat: String, lng: String,
    onLatChange: (String) -> Unit, onLngChange: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = lat, onValueChange = onLatChange,
            label = { Text("Latitude") }, modifier = Modifier.weight(1f), singleLine = true)
        OutlinedTextField(value = lng, onValueChange = onLngChange,
            label = { Text("Longitude") }, modifier = Modifier.weight(1f), singleLine = true)
    }
}

@Composable
fun EnvTabLayout(
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
            Text(if (isLoading) "Fetching..." else "Fetch Data")
        }
        if (result.isNotEmpty()) {
            Text("Response:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                text = result,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1B2A), RoundedCornerShape(6.dp))
                    .padding(10.dp),
                color = Color(0xFF7DF9FF)
            )
        }
    }
}

// ── API Calls ──────────────────────────────────────────────────────────────────

private suspend fun callAirQualityApi(apiKey: String, lat: Double, lng: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("location", JSONObject().apply {
                    put("latitude", lat)
                    put("longitude", lng)
                })
                put("extraComputations", listOf("HEALTH_RECOMMENDATIONS", "DOMINANT_POLLUTANT_CONCENTRATION"))
                put("languageCode", "en")
            }.toString()

            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://airquality.googleapis.com/v1/currentConditions:lookup?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "(empty)"
            val parsed = runCatching { JSONObject(responseBody) }.getOrNull() ?: return@withContext responseBody

            val sb = StringBuilder()
            val indexes = parsed.optJSONArray("indexes")
            if (indexes != null) {
                sb.appendLine("Air Quality Indexes:")
                for (i in 0 until indexes.length()) {
                    val idx = indexes.getJSONObject(i)
                    sb.appendLine("  ${idx.optString("displayName")}: ${idx.optInt("aqi")} — ${idx.optString("category")}")
                }
            }
            val pollutants = parsed.optJSONArray("pollutants")
            if (pollutants != null) {
                sb.appendLine("\nPollutants:")
                for (i in 0 until pollutants.length()) {
                    val p = pollutants.getJSONObject(i)
                    val conc = p.optJSONObject("concentration")
                    sb.appendLine("  ${p.optString("displayName")}: ${conc?.optDouble("value")} ${conc?.optString("units")}")
                }
            }
            val health = parsed.optJSONObject("healthRecommendations")
            if (health != null) {
                sb.appendLine("\nHealth Advice:")
                sb.appendLine("  General: ${health.optString("generalPopulation").take(120)}...")
            }
            sb.toString().ifEmpty { responseBody }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

private suspend fun callPollenApi(apiKey: String, lat: Double, lng: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://pollen.googleapis.com/v1/forecast:lookup" +
                    "?location.latitude=$lat&location.longitude=$lng&days=1&key=$apiKey"

            val client = OkHttpClient()
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val responseBody = response.body?.string() ?: "(empty)"
            val parsed = runCatching { JSONObject(responseBody) }.getOrNull() ?: return@withContext responseBody

            val sb = StringBuilder()
            val dailyInfo = parsed.optJSONArray("dailyInfo")
            if (dailyInfo != null && dailyInfo.length() > 0) {
                val day = dailyInfo.getJSONObject(0)
                val date = day.optJSONObject("date")
                sb.appendLine("Pollen Forecast for ${date?.optInt("year")}-${date?.optInt("month")}-${date?.optInt("day")}\n")
                val pollenTypes = day.optJSONArray("pollenTypeInfo")
                if (pollenTypes != null) {
                    sb.appendLine("Pollen Types:")
                    for (i in 0 until pollenTypes.length()) {
                        val p = pollenTypes.getJSONObject(i)
                        val index = p.optJSONObject("indexInfo")
                        sb.appendLine("  ${p.optString("displayName")}: ${index?.optString("category") ?: "N/A"} (${index?.optInt("value") ?: 0})")
                    }
                }
                val plants = day.optJSONArray("plantInfo")
                if (plants != null) {
                    sb.appendLine("\nPlant Sources:")
                    for (i in 0 until plants.length()) {
                        val p = plants.getJSONObject(i)
                        val index = p.optJSONObject("indexInfo")
                        if ((index?.optInt("value") ?: 0) > 0) {
                            sb.appendLine("  ${p.optString("displayName")}: ${index?.optString("category")} (${index?.optInt("value")})")
                        }
                    }
                }
            }
            sb.toString().ifEmpty { responseBody }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

private suspend fun callSolarApi(apiKey: String, lat: Double, lng: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://solar.googleapis.com/v1/buildingInsights:findClosest" +
                    "?location.latitude=$lat&location.longitude=$lng&requiredQuality=LOW&key=$apiKey"

            val client = OkHttpClient()
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val responseBody = response.body?.string() ?: "(empty)"
            val parsed = runCatching { JSONObject(responseBody) }.getOrNull() ?: return@withContext responseBody

            val sb = StringBuilder()
            val stats = parsed.optJSONObject("solarPotential")
            if (stats != null) {
                sb.appendLine("Rooftop Area: ${stats.optDouble("wholeRoofStats").let { "see below" }}")
                val roofStats = stats.optJSONObject("wholeRoofStats")
                sb.appendLine("  Area: ${roofStats?.optDouble("areaMeters2")} m²")
                sb.appendLine("  Sunshine Hours/Year: ${roofStats?.optDouble("sunshineQuantiles")?.let { "varies" }}")
                sb.appendLine("\nMax Solar Panels: ${stats.optInt("maxArrayPanelsCount")}")
                sb.appendLine("Max Array Area: ${stats.optDouble("maxArrayAreaMeters2")} m²")
                sb.appendLine("Max Sunshine Hours/Year: ${stats.optDouble("maxSunshineHoursPerYear")} hrs")
                sb.appendLine("Carbon Offset/Year: ${stats.optDouble("carbonOffsetFactorKgPerMwh")} kg/MWh")
                val configs = stats.optJSONArray("solarPanelConfigs")
                if (configs != null && configs.length() > 0) {
                    sb.appendLine("\nBest Panel Config:")
                    val best = configs.getJSONObject(configs.length() - 1)
                    sb.appendLine("  Panels: ${best.optInt("panelsCount")}")
                    sb.appendLine("  Yearly Energy: ${best.optDouble("yearlyEnergyDcKwh")} kWh/year")
                }
            }
            sb.toString().ifEmpty { responseBody }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

private suspend fun callWeatherApi(apiKey: String, lat: Double, lng: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://weather.googleapis.com/v1/forecast/hours:lookup" +
                    "?location.latitude=$lat&location.longitude=$lng&hours=6&key=$apiKey"

            val client = OkHttpClient()
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val responseBody = response.body?.string() ?: "(empty)"
            val parsed = runCatching { JSONObject(responseBody) }.getOrNull() ?: return@withContext responseBody

            val sb = StringBuilder()
            val forecasts = parsed.optJSONArray("forecastHours")
            if (forecasts != null) {
                sb.appendLine("Hourly Weather Forecast:\n")
                for (i in 0 until forecasts.length()) {
                    val hour = forecasts.getJSONObject(i)
                    val interval = hour.optJSONObject("interval")
                    val startTime = interval?.optString("startTime") ?: ""
                    val temp = hour.optJSONObject("temperature")
                    val tempVal = temp?.optDouble("degrees")
                    val tempUnit = temp?.optString("unit") ?: "CELSIUS"
                    val condition = hour.optJSONObject("weatherCondition")?.optString("description") ?: ""
                    val humidity = hour.optJSONObject("relativeHumidity")?.optInt("percent") ?: 0
                    val wind = hour.optJSONObject("wind")
                    val windSpeed = wind?.optJSONObject("speed")?.optDouble("value") ?: 0.0
                    sb.appendLine("${startTime.take(16).replace("T", " ")}")
                    sb.appendLine("  Temp: ${tempVal}° $tempUnit  |  Humidity: $humidity%")
                    sb.appendLine("  Condition: $condition")
                    sb.appendLine("  Wind: $windSpeed m/s\n")
                }
            }
            sb.toString().ifEmpty { "HTTP ${response.code}\n$responseBody" }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
