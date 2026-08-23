package com.simats.skillora.ui.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.components.AuthButton
import com.simats.skillora.ui.components.AuthTextField
import com.simats.skillora.ui.components.GoogleButton
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoogleSignIn: suspend () -> Result<Boolean>,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    AuthLayout(
        activeTab = "login",
        onTabChange = { if (it == "register") onNavigateToRegister() }
    ) {
        Column {
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "student@campus.edu",
                modifier = Modifier.padding(bottom = 24.dp)
            )

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "••••••••",
                isPassword = true
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Forgot Password?",
                    color = Color(0x8CE7E9E6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onNavigateToForgotPassword() }
                )
            }

            AuthButton(label = "Login", onPress = { onLoginSuccess() })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x1AFFFFFF))
                Text(
                    text = "OR",
                    color = Color(0x80FFFFFF),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x1AFFFFFF))
            }

            var isLoading by remember { mutableStateOf(false) }

            GoogleButton(
                onPress = {
                    scope.launch {
                        isLoading = true
                        val result = onGoogleSignIn()
                        isLoading = false
                        if (result.isFailure) {
                            Toast.makeText(
                                context,
                                "Google Sign-In failed: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                loading = isLoading
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Don't have an account? ", color = Color(0x99F2F3F1), fontSize = 16.sp)
                Text(
                    text = "Register",
                    color = Color(0xCCE7E9E6),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}
