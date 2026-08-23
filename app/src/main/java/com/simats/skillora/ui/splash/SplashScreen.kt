package com.simats.skillora.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val opacity = remember { Animatable(0f) }
    val translateY = remember { Animatable(10f) }

    LaunchedEffect(Unit) {
        launch {
            opacity.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f))
            )
        }
        launch {
            translateY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 450, easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f))
            )
        }
        delay(2000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(bottom = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(opacity.value)
                .offset(y = translateY.value.dp)
        ) {
            // Logo Wrap
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(Surface, RoundedCornerShape(6.dp))
                    .padding(top = 10.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = TextColor,
                            modifier = Modifier.size(34.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = TextColor,
                            modifier = Modifier
                                .size(12.dp)
                                .offset(x = 10.dp, y = (-10).dp) // Adjust based on visual
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "LUMINARY EXCHANGE",
                        color = Outline,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Skillora",
                color = TextColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Learn. Teach. Grow.",
                color = TextMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = TextMuted,
                strokeWidth = 3.dp
            )
        }

        // Footer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp, top = 20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "CAMPUS SKILL EXCHANGE",
                color = TextMuted,
                fontSize = 14.sp,
                letterSpacing = 4.sp
            )
        }
    }
}
