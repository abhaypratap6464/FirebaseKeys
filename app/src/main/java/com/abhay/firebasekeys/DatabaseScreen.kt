package com.abhay.firebasekeys

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun DatabaseScreen(modifier: Modifier = Modifier) {
    LocalContext.current
    val databaseUrl = NativeKeys.getFirebaseDatabaseUrl()
    val apiKey = NativeKeys.getGoogleApiKey()

    val scope = rememberCoroutineScope()
    var path        by remember { mutableStateOf("/") }
    var response    by remember { mutableStateOf("") }
    var statusCode  by remember { mutableStateOf(0) }
    var isLoading   by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Database Data Downloader", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // Warning banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD32F2F), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text("Security Risk Demo", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "This calls the Firebase Realtime Database REST API using\n" +
                        "only the database URL from strings.xml — no authentication.\n" +
                        "If security rules allow public read, ALL data is exposed.",
                color = Color.White, fontSize = 12.sp
            )
        }

        // Endpoint preview
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF212121), RoundedCornerShape(6.dp))
                .padding(10.dp)
        ) {
            Text("REST endpoint being called:", fontSize = 11.sp, color = Color(0xFFAAAAAA), fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "GET $databaseUrl${path.trimEnd('/')}.json",
                fontSize = 10.sp, color = Color(0xFF80FF80), fontFamily = FontFamily.Monospace
            )
        }

        // Path input
        OutlinedTextField(
            value = path,
            onValueChange = { path = it },
            label = { Text("Database Path") },
            placeholder = { Text("/users  or  /orders/2024  etc.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Presets
        Text("Quick paths:", fontSize = 12.sp, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("/", "/users", "/orders", "/config").forEach { preset ->
                FilterChip(
                    selected = path == preset,
                    onClick = { path = preset },
                    label = { Text(preset, fontSize = 11.sp) }
                )
            }
        }

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    val result = downloadDatabasePath(databaseUrl, path, apiKey)
                    statusCode = result.first
                    response   = result.second
                    isLoading  = false
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isLoading) "Downloading..." else "Download Data (No Auth)")
        }

        if (response.isNotEmpty()) {
            // Status badge
            val isSuccess = statusCode in 200..299
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFB71C1C),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "HTTP $statusCode",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    color = if (isSuccess) Color(0xFF1B5E20) else Color(0xFF7F0000),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isSuccess) "Data accessible — rules are open!" else "Access denied",
                        color = Color.White, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Formatted response
            Text("Response:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                text = response,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117), RoundedCornerShape(6.dp))
                    .padding(12.dp),
                color = if (isSuccess) Color(0xFF80FF80) else Color(0xFFFF6B6B)
            )

            // What an attacker could do section
            if (isSuccess) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFFD32F2F), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "What an attacker can do with this:",
                        fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "# Download entire database with one curl command:\n" +
                                "curl \"$databaseUrl/.json\" > database_dump.json\n\n" +
                                "# Read a specific path:\n" +
                                "curl \"$databaseUrl/users.json\"\n\n" +
                                "# Write data (if rules allow write too):\n" +
                                "curl -X PUT \"$databaseUrl/hacked.json\" \\\n" +
                                "  -d '\"owned\"'",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF8B0000),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    )
                }
            }
        }

        // Fix section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text("How to protect your database:", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = """// INSECURE rules (default — anyone can read/write):
{
  "rules": {
    ".read": true,
    ".write": true
  }
}

// SECURE rules (require authentication):
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}""",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF1B5E20),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC8E6C9), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            )
        }
    }
}

private suspend fun downloadDatabasePath(
    databaseUrl: String,
    path: String,
    apiKey: String
): Pair<Int, String> {
    return withContext(Dispatchers.IO) {
        try {
            val cleanPath = path.trim().trimEnd('/')
            val url = "$databaseUrl$cleanPath.json?auth=$apiKey"
            val client = OkHttpClient()
            val response = client.newCall(Request.Builder().url(url).get().build()).execute()
            val code = response.code
            val body = response.body?.string() ?: "(empty response)"
            val formatted = prettyPrint(body)
            Pair(code, formatted)
        } catch (e: Exception) {
            Pair(0, "Error: ${e.message}")
        }
    }
}

private fun prettyPrint(json: String): String {
    return try {
        when {
            json.trimStart().startsWith("{") ->
                JSONObject(json).toString(2)
            json.trimStart().startsWith("[") ->
                JSONArray(json).toString(2)
            else -> json
        }
    } catch (e: Exception) {
        json
    }
}
