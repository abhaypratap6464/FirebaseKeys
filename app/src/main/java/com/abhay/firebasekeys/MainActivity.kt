package com.abhay.firebasekeys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhay.firebasekeys.ui.theme.FirebaseKeysTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FirebaseKeysTheme {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                                icon = {}, label = { Text("Keys") })
                            NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                                icon = {}, label = { Text("Map") })
                            NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                                icon = {}, label = { Text("Firebase") })
                            NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 },
                                icon = {}, label = { Text("Database") })
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> FirebaseKeysDemoScreen(modifier = Modifier.padding(innerPadding))
                        1 -> MapScreen(modifier = Modifier.padding(innerPadding))
                        2 -> FirebaseAppScreen(modifier = Modifier.padding(innerPadding))
                        3 -> DatabaseScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun FirebaseKeysDemoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Read the exposed keys directly from strings.xml — just like an attacker would after decompiling
    val apiKey = context.getString(R.string.google_maps_api_key)
    val appId = context.getString(R.string.firebase_project_id)

    var firestoreResult by remember { mutableStateOf("Not tested yet") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Firebase Key Exposure Demo",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // --- INSECURE SECTION ---
        SectionCard(
            title = "INSECURE: Keys in strings.xml",
            borderColor = Color(0xFFD32F2F),
            backgroundColor = Color(0xFFFFEBEE)
        ) {
            Text(
                text = "These values are read from res/values/strings.xml\n" +
                        "Anyone can extract them by running:\n" +
                        "  apktool d yourapp.apk",
                fontSize = 13.sp,
                color = Color(0xFF8B0000)
            )
            Spacer(modifier = Modifier.height(8.dp))
            KeyRow("google_api_key", apiKey)
            KeyRow("google_app_id", appId)
        }

        // --- ATTACK DEMO SECTION ---
        SectionCard(
            title = "What an attacker can do with these keys",
            borderColor = Color(0xFFE65100),
            backgroundColor = Color(0xFFFFF3E0)
        ) {
            Text(
                text = "Using just the API key + project ID, anyone can call Firebase REST APIs " +
                        "without installing your app.",
                fontSize = 13.sp,
                color = Color(0xFF7F3B00)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        firestoreResult = testFirestoreAccess(apiKey)
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text(if (isLoading) "Testing..." else "Test Unauthorized Access")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Firestore REST API response:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            ResultBox(firestoreResult)
        }

        // --- APKTOOL SECTION ---
        SectionCard(
            title = "How to extract keys from any APK",
            borderColor = Color(0xFF6A1B9A),
            backgroundColor = Color(0xFFF3E5F5)
        ) {
            Text(
                text = "Step 1 — Decompile the APK:",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4A148C)
            )
            ResultBox("$ apktool d yourapp.apk")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Step 2 — Find the keys:",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4A148C)
            )
            ResultBox("$ grep -r 'api_key\\|maps_key\\|firebase' yourapp/res/values/")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Step 3 — Use the key in any HTTP client:",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4A148C)
            )
            ResultBox("$ curl \"https://maps.googleapis.com/maps/api/geocode/json\\\n  ?address=Delhi&key=STOLEN_KEY\"")
        }

        // --- SECURE SECTION ---
        SectionCard(
            title = "SECURE: The right way",
            borderColor = Color(0xFF2E7D32),
            backgroundColor = Color(0xFFE8F5E9)
        ) {
            Text(
                text = "1. Use google-services.json (not strings.xml)\n" +
                        "2. Restrict API keys in Google Cloud Console:\n" +
                        "   - Restrict to your app's SHA-1 fingerprint\n" +
                        "   - Restrict to specific APIs only\n" +
                        "3. Set strict Firebase Security Rules:\n" +
                        "   - Never use allow read, write: if true;\n" +
                        "   - Require authentication\n" +
                        "4. Never commit google-services.json to public repos",
                fontSize = 13.sp,
                color = Color(0xFF1B5E20)
            )
        }
    }
}

private suspend fun testFirestoreAccess(apiKey: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/identitytoolkit/v3/relyingparty/getProjectConfig?key=$apiKey"
            val client = OkHttpClient()
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string()?.take(300) ?: "(empty)"
            "HTTP $code\n$body"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}


@Composable
private fun SectionCard(
    title: String,
    borderColor: Color,
    backgroundColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        content = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = borderColor)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    )
}

@Composable
private fun KeyRow(name: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
        Text(
            text = if (value.length > 20) value.take(20) + "…" else value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFD32F2F)
        )
    }
}

@Composable
private fun ResultBox(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF212121), RoundedCornerShape(4.dp))
            .padding(8.dp),
        color = Color(0xFF80FF80)
    )
}
