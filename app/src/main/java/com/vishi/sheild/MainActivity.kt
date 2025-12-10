package com.vishi.sheild
import kotlin.math.*

import androidx.compose.foundation.clickable
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vishi.sheild.ui.theme.SHEILDTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var pressCount = 0
    private var lastPressTime = 0L

    // ✅ Each phone's user name (default fallback)
    private var userName: String = "SHEILD User"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make layout resize when keyboard appears (so text field stays visible)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            SHEILDTheme {
                var currentScreen by remember { mutableStateOf("home") }

                // This state holds the text typed in the "Your Name" field
                var nameState by remember { mutableStateOf(userName) }

                when (currentScreen) {
                    "home" -> HomeScreen(
                        name = nameState,
                        onNameChange = { newName ->
                            nameState = newName
                            // keep a safe fallback if they leave it blank
                            userName = if (newName.isBlank()) "SHEILD User" else newName
                        },
                        onVictimClick = { currentScreen = "victim" },
                        onRescuerClick = { currentScreen = "rescuer" }
                    )

                    "victim" -> SosScreen(
                        onSosClick = { triggerSos() },
                        onBack = { currentScreen = "home" }
                    )

                    "rescuer" -> RescuerScreen(
                        onBack = { currentScreen = "home" }
                    )
                }
            }
        }
    }

    // Handle Volume Down triple-press while app is open (Victim mode)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastPressTime < 1500) {
                pressCount++
            } else {
                pressCount = 1
            }

            lastPressTime = currentTime

            if (pressCount == 3) {
                pressCount = 0
                triggerSos()
            }

            // consume the event
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun triggerSos() {
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine) {
            getLocationAndShow()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getLocationAndShow() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val lat = location.latitude
                        val lng = location.longitude

                        val msg = "SOS Location: $lat, $lng"
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

                        // 🔥 Send alert to Firebase
                        sendAlertToCloud(lat, lng)

                    } else {
                        Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendAlertToCloud(lat: Double, lng: Double) {
        val db = Firebase.firestore

        val safeName = if (userName.isBlank()) "SHEILD User" else userName

        val alert = hashMapOf(
            "victimName" to safeName,                 // ✅ dynamic per phone
            "timestamp" to System.currentTimeMillis(),
            "lat" to lat,
            "lng" to lng
        )

        db.collection("alerts")
            .add(alert)
            .addOnSuccessListener {
                Toast.makeText(this, "Alert sent to cloud", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send alert", Toast.LENGTH_SHORT).show()
            }
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                getLocationAndShow()
            } else {
                Toast.makeText(
                    this,
                    "Location permission is required for SOS",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
}

// 🔹 Util: time formatting for clock + rescuer list
fun getCurrentTimeString(): String {
    val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())  // 12-hr with AM/PM
    return sdf.format(Date())
}

// 🔹 IST + 12-hour time with AM/PM for rescuer dashboard
fun formatTimestamp(ts: Long): String {
    val sdf = SimpleDateFormat("dd MMM yy • hh:mm a", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")  // IST
    return sdf.format(Date(ts))
}

// 🔹 HOME SCREEN: choose Victim / Rescuer mode + Name input
@Composable
fun HomeScreen(
    name: String,
    onNameChange: (String) -> Unit,
    onVictimClick: () -> Unit,
    onRescuerClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),  // scrollable + works well with keyboard
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "SHEILD",
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose how you want to use the app",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            // 🔹 Name input
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Your name (for alerts)") },
                placeholder = { Text("Enter Name") },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.Black,     // 👈 typed text in black
                    fontSize = 16.sp
                ),
                modifier = Modifier.fillMaxWidth(0.9f)
            )


            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onVictimClick,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEC1C24),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "I NEED HELP (VICTIM)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onRescuerClick,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E88E5),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "I WANT TO HELP (RESCUER)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 🔹 VICTIM SCREEN (watch UI) with Back
@Composable
fun SosScreen(
    onSosClick: () -> Unit,
    onBack: () -> Unit
) {
    var currentTime by remember { mutableStateOf(getCurrentTimeString()) }

    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeString()
            delay(1000L)
        }
    }

    val lightBackground = Color(0xFFF6F6F6)
    val watchBackground = Color(0xFF121212)
    val sosRed = Color(0xFFEC1C24)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {

            // Top: back
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Back",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable { onBack() }
                )
            }

            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SHEILD",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Women Safety Watch",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Watch face
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .background(watchBackground)
                    .border(
                        width = 4.dp,
                        color = sosRed,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LIVE TIME",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentTime,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "S O S  R E A D Y",
                        style = MaterialTheme.typography.bodySmall,
                        color = sosRed
                    )
                }
            }

            // SOS button + hints
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onSosClick,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(60.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = sosRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "TRIGGER S O S",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Press Volume Down 3 times\nfor a silent SOS trigger.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Stay calm. SHEILD is watching over you.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Data holder for rescuer UI
data class AlertItem(
    val id: String,
    val victimName: String,
    val timestamp: Long,
    val lat: Double,
    val lng: Double
)

fun distanceInMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val R = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}


// 🔹 RESCUER SCREEN
@Composable
fun RescuerScreen(
    onBack: () -> Unit
) {
    val db = Firebase.firestore
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var allAlerts by remember { mutableStateOf<List<AlertItem>>(emptyList()) }
    var rescuerLat by remember { mutableStateOf<Double?>(null) }
    var rescuerLng by remember { mutableStateOf<Double?>(null) }

    // Get rescuer's current location
    LaunchedEffect("rescuerLocation") {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine) {
            Toast.makeText(
                context,
                "Turn on location permission to see nearby alerts",
                Toast.LENGTH_SHORT
            ).show()
            return@LaunchedEffect
        }

        fusedClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    rescuerLat = loc.latitude
                    rescuerLng = loc.longitude
                }
            }
    }

    // Listen to Firestore in real-time (last 24 hours)
    LaunchedEffect(Unit) {
        db.collection("alerts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    return@addSnapshotListener
                }

                val now = System.currentTimeMillis()
                val cutoff = now - 24L * 60L * 60L * 1000L   // 24 hours in ms

                allAlerts = snapshot.documents
                    .mapNotNull { doc ->
                        val victimName = doc.getString("victimName") ?: "Unknown"
                        val ts = doc.getLong("timestamp") ?: 0L
                        val lat = doc.getDouble("lat") ?: return@mapNotNull null
                        val lng = doc.getDouble("lng") ?: return@mapNotNull null
                        AlertItem(
                            id = doc.id,
                            victimName = victimName,
                            timestamp = ts,
                            lat = lat,
                            lng = lng
                        )
                    }
                    .filter { it.timestamp >= cutoff }
            }
    }

    // Filter alerts within 2 km of rescuer
    val alerts = remember(allAlerts, rescuerLat, rescuerLng) {
        if (rescuerLat == null || rescuerLng == null) {
            emptyList()
        } else {
            allAlerts.filter {
                distanceInMeters(
                    rescuerLat!!,
                    rescuerLng!!,
                    it.lat,
                    it.lng
                ) <= 2000.0   // 2000 meters = 2 km
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Back",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clickable { onBack() }
                )
                Text(
                    text = "Rescuer Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(40.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (rescuerLat == null || rescuerLng == null) {
                Text(
                    text = "Fetching your location...\nTurn on GPS to see nearby alerts.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Showing alerts within 2 km of you",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No nearby SOS alerts right now.\nYou’ll see them here in real time.",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alerts) { alert ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "From: ${alert.victimName}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "When: ${formatTimestamp(alert.timestamp)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Location: ${alert.lat}, ${alert.lng}",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val uri = Uri.parse(
                                            "geo:${alert.lat},${alert.lng}?q=${alert.lat},${alert.lng}(SOS Alert)"
                                        )
                                        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(mapIntent)
                                        } else {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    uri
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(text = "Open in Maps")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
