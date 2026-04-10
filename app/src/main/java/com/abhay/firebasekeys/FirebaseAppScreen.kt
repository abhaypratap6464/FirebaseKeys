package com.abhay.firebasekeys

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Demonstrates the equivalent of:
 *
 *   firebaseAppToMap(FirebaseApp)  — reads all options out of a FirebaseApp
 *   readableMapToFirebaseApp(...)  — initialises a FirebaseApp from a config map
 *
 * Both operations are done here using only the keys extracted from strings.xml,
 * proving that anyone who decompiles the APK can fully reconstruct and use
 * the Firebase project configuration.
 */
@Composable
fun FirebaseAppScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // ── readableMapToFirebaseApp equivalent ──────────────────────────────────
    // Keys are now loaded from the native library — no plaintext in resources
    val apiKey = NativeKeys.getGoogleApiKey()
    val appId = NativeKeys.getFirebaseAppId()
    val projectId = NativeKeys.getFirebaseProjectId()
    val databaseUrl = NativeKeys.getFirebaseDatabaseUrl()
    val storageBucket = NativeKeys.getFirebaseStorageBucket()
    val senderId = NativeKeys.getFirebaseMessagingSenderId()
    // gaTrackingId is NOT hardcoded — we read it back from FirebaseOptions after init

    var firebaseAppMap  by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var initError       by remember { mutableStateOf<String?>(null) }
    var initialized     by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Firebase App Inspector", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // ── Step 1: Keys extracted from strings.xml ──────────────────────────
        SectionCard(
            title = "Step 1 — Keys extracted from strings.xml",
            borderColor = Color(0xFFD32F2F),
            backgroundColor = Color(0xFFFFEBEE)
        ) {
            ConfigRow("google_api_key",            apiKey)
            ConfigRow("google_app_id",             appId)
            ConfigRow("firebase_project_id",       projectId)
            ConfigRow("firebase_database_url",     databaseUrl)
            ConfigRow("firebase_storage_bucket",   storageBucket)
            ConfigRow("firebase_messaging_sender", senderId)
            ConfigRow("ga_tracking_id",            "(read from FirebaseOptions after init ↓)")
        }

        // ── Step 2: readableMapToFirebaseApp ─────────────────────────────────
        SectionCard(
            title = "Step 2 — readableMapToFirebaseApp()",
            borderColor = Color(0xFFE65100),
            backgroundColor = Color(0xFFFFF3E0)
        ) {
            Text(
                text = "Initialise a real FirebaseApp instance using only the\n" +
                        "stolen config values — no google-services.json needed.",
                fontSize = 12.sp, color = Color(0xFF7F3B00)
            )
            Spacer(Modifier.height(10.dp))

            CodeBlock(
                """val options = FirebaseOptions.Builder()
    .setApiKey(apiKey)
    .setApplicationId(appId)
    .setProjectId(projectId)
    .setDatabaseUrl(databaseUrl)
    .setStorageBucket(storageBucket)
    .setGcmSenderId(senderId)
    .build()

FirebaseApp.initializeApp(context, options, "stolen-app")"""
            )

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    initError = null
                    try {
                        val existing = runCatching {
                            FirebaseApp.getInstance("stolen-app")
                        }.getOrNull()

                        val app = existing ?: run {
                            val options = FirebaseOptions.Builder()
                                .setApiKey(apiKey)
                                .setApplicationId(appId)
                                .setProjectId(projectId)
                                .setDatabaseUrl(databaseUrl)
                                .setStorageBucket(storageBucket)
                                .setGcmSenderId(senderId)
                                .build()
                            FirebaseApp.initializeApp(context, options, "stolen-app")
                        }

                        // ── firebaseAppToMap equivalent ──────────────────────
                        firebaseAppMap = firebaseAppToMap(app)
                        initialized = true
                    } catch (e: Exception) {
                        initError = e.message
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Initialize FirebaseApp from stolen keys")
            }

            if (initError != null) {
                Spacer(Modifier.height(6.dp))
                Text(initError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }

        // ── Step 3: firebaseAppToMap output ──────────────────────────────────
        if (initialized && firebaseAppMap.isNotEmpty()) {
            SectionCard(
                title = "Step 3 — firebaseAppToMap() output",
                borderColor = Color(0xFF1565C0),
                backgroundColor = Color(0xFFE3F2FD)
            ) {
                Text(
                    text = "FirebaseApp successfully initialised!\nAll options read back from the live instance:",
                    fontSize = 12.sp, color = Color(0xFF0D47A1)
                )
                Spacer(Modifier.height(8.dp))

                val options  = firebaseAppMap["options"]  as? Map<*, *> ?: emptyMap<Any, Any>()
                val appConfig = firebaseAppMap["appConfig"] as? Map<*, *> ?: emptyMap<Any, Any>()

                Text("appConfig:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0D47A1))
                appConfig.forEach { (k, v) -> ConfigRow(k.toString(), v.toString()) }

                Spacer(Modifier.height(6.dp))
                Text("options:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0D47A1))
                options.forEach { (k, v) -> ConfigRow(k.toString(), v?.toString() ?: "null") }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "This is exactly what react-native-firebase's\n" +
                            "ReactNativeFirebaseAppModule exposes to JS —\n" +
                            "all derived purely from hardcoded strings.xml values.",
                    fontSize = 11.sp, color = Color(0xFF0D47A1)
                )
            }

            // ── Fix ──────────────────────────────────────────────────────────
            SectionCard(
                title = "How to fix this",
                borderColor = Color(0xFF2E7D32),
                backgroundColor = Color(0xFFE8F5E9)
            ) {
                Text(
                    text = "1. Use google-services.json — not strings.xml\n" +
                            "2. Add google-services.json to .gitignore\n" +
                            "3. Restrict API keys in Google Cloud Console\n" +
                            "   (SHA-1 fingerprint + package name restriction)\n" +
                            "4. Enforce Firebase Security Rules — require auth\n" +
                            "5. Store secrets in CI/CD env vars, not source code",
                    fontSize = 13.sp, color = Color(0xFF1B5E20)
                )
            }
        }
    }
}

/**
 * Equivalent of ReactNativeFirebaseAppModule.firebaseAppToMap()
 */
private fun firebaseAppToMap(firebaseApp: FirebaseApp): Map<String, Any?> {
    val name    = firebaseApp.name
    val options = firebaseApp.options

    val optionsMap = mapOf(
        "apiKey"            to options.apiKey,
        "appId"             to options.applicationId,
        "projectId"         to options.projectId,
        "databaseURL"       to options.databaseUrl,
        "gaTrackingId"      to options.gaTrackingId,
        "messagingSenderId" to options.gcmSenderId,
        "storageBucket"     to options.storageBucket
    )

    val appConfigMap = mapOf(
        "name"                           to name,
        "automaticDataCollectionEnabled" to firebaseApp.isDataCollectionDefaultEnabled
    )

    return mapOf(
        "options"   to optionsMap,
        "appConfig" to appConfigMap
    )
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
            .padding(12.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = borderColor)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ConfigRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray,
            modifier = Modifier.weight(0.45f))
        Text(
            text = if (value.length > 28) value.take(28) + "…" else value,
            fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFD32F2F),
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    Text(
        text = code,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(6.dp))
            .padding(10.dp),
        color = Color(0xFF80FF80)
    )
}
