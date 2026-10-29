package com.example.ui.screens.common

import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*

@Composable
fun VideoCallScreen(
    title: String,
    roomCode: String,
    isHost: Boolean,
    onLeaveCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMicMuted by remember { mutableStateOf(false) }
    var isVideoDisabled by remember { mutableStateOf(false) }
    var showWebviewMeet by remember { mutableStateOf(false) }

    val jitsiUrl = "https://meet.jit.si/$roomCode#config.prejoinPageEnabled=false"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0910))
    ) {
        if (showWebviewMeet) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = WebViewClient()
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }
                        }
                        loadUrl(jitsiUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Interactive Native Video Classroom Mockup
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Room Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SlatyMint)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LIVE CLASSROOM",
                                color = SlatyMint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Room: $roomCode",
                            color = SlatyTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF201C2E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF332D48))
                    ) {
                        TextButton(onClick = { showWebviewMeet = !showWebviewMeet }) {
                            Text(
                                text = if (showWebviewMeet) "Native View" else "Open Jitsi Meet",
                                color = SlatyLavender,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Stage Video (Tutor / Screen)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF151320))
                        .border(1.dp, SlatyCardBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF241F35))
                                .border(3.dp, SlatyMint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isHost) "YOU" else "DR",
                                color = SlatyMint,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isHost) "You (Hosting Lecture)" else "Dr. Emmanuel Okon (Speaking)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Audio & Video Connected • HD 1080p",
                            color = SlatyMint,
                            fontSize = 12.sp
                        )
                    }

                    // Self PIP Tile in corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                            .size(width = 110.dp, height = 150.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF28243A))
                            .border(1.dp, Color(0xFF443D60), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVideoDisabled) {
                            Icon(Icons.Default.VideocamOff, contentDescription = "Cam off", tint = Color.Gray)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Person, contentDescription = "Self", tint = SlatyMint, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Self", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Control Bar
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1F1C2F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF332D48)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic toggle
                        IconButton(
                            onClick = { isMicMuted = !isMicMuted },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isMicMuted) SlatyError.copy(alpha = 0.2f) else Color(0xFF2C2740))
                        ) {
                            Icon(
                                imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = if (isMicMuted) SlatyError else Color.White
                            )
                        }

                        // Video toggle
                        IconButton(
                            onClick = { isVideoDisabled = !isVideoDisabled },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isVideoDisabled) SlatyError.copy(alpha = 0.2f) else Color(0xFF2C2740))
                        ) {
                            Icon(
                                imageVector = if (isVideoDisabled) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                contentDescription = "Camera",
                                tint = if (isVideoDisabled) SlatyError else Color.White
                            )
                        }

                        // Leave / End Call button
                        IconButton(
                            onClick = onLeaveCall,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(SlatyError)
                                .testTag("leave_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
