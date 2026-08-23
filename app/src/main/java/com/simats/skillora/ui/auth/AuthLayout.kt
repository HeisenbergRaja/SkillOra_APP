package com.simats.skillora.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.theme.*

@Composable
fun AuthLayout(
    activeTab: String,
    onTabChange: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val tabProgress by animateFloatAsState(
        targetValue = if (activeTab == "login") 0f else 1f,
        animationSpec = tween(durationMillis = 260, easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)),
        label = "tabProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Section
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Wrap
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(Surface, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = TextColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = TextColor,
                            modifier = Modifier
                                .size(10.dp)
                                .offset(x = 10.dp, y = (-6).dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "LUMINARY EXCHANGE",
                    color = TextMuted,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Skillora",
                    color = Primary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 52.sp
                )
            }

            // Tabs Wrap
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0x8C20271E), RoundedCornerShape(26.dp))
                    .padding(6.dp)
            ) {
                // Indicator
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val width = maxWidth / 2
                    Box(
                        modifier = Modifier
                            .width(width)
                            .fillMaxHeight()
                            .offset(x = width * tabProgress)
                            .background(Color(0x3DE7E9E6), RoundedCornerShape(22.dp))
                    )
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabChange("login") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Login",
                            color = if (activeTab == "login") Primary else TextMuted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabChange("register") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Register",
                            color = if (activeTab == "register") Primary else TextMuted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (activeTab == "login") "Welcome Back" else "Create Account",
                color = Primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Text(
                text = if (activeTab == "login") "Sign in to continue learning" else "Join your campus learning community",
                color = Color(0x99F2F3F1),
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, bottom = 24.dp)
            )
        }

        // Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            content()
        }
    }
}
