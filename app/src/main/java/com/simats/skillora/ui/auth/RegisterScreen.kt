package com.simats.skillora.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.components.AuthButton
import com.simats.skillora.ui.components.AuthTextField
import com.simats.skillora.ui.components.SelectInput
import com.simats.skillora.ui.theme.Primary
import com.simats.skillora.ui.theme.Surface
import com.simats.skillora.ui.theme.TextColor

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("Freshman") }

    var showYearModal by remember { mutableStateOf(false) }

    AuthLayout(
        activeTab = "register",
        onTabChange = { if (it == "login") onNavigateToLogin() }
    ) {
        Column {
            AuthTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Full Name",
                placeholder = "Jane Doe",
                modifier = Modifier.padding(bottom = 24.dp)
            )
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "jane@college.edu",
                modifier = Modifier.padding(bottom = 24.dp)
            )
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "••••••••",
                isPassword = true,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                placeholder = "••••••••",
                isPassword = true,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            AuthTextField(
                value = college,
                onValueChange = { college = it },
                label = "College Name",
                placeholder = "State University",
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AuthTextField(
                    value = dept,
                    onValueChange = { dept = it },
                    label = "Department",
                    placeholder = "Design",
                    modifier = Modifier.weight(1f)
                )
                SelectInput(
                    label = "Year",
                    value = year,
                    placeholder = "Select year",
                    onPress = { showYearModal = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AuthButton(label = "Register", onPress = { onRegisterSuccess() })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Already have an account? ", color = Color(0x99F2F3F1), fontSize = 16.sp)
                Text(
                    text = "Login",
                    color = Color(0xCCE7E9E6),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }

    if (showYearModal) {
        AlertDialog(
            onDismissRequest = { showYearModal = false },
            containerColor = Surface,
            title = { Text("Select year", color = Primary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    listOf("Freshman", "Sophomore", "Junior", "Senior").forEach { y ->
                        Text(
                            text = y,
                            color = TextColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    year = y
                                    showYearModal = false
                                }
                                .padding(vertical = 16.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        HorizontalDivider(color = Color(0x14F2F3F1))
                    }
                }
            },
            confirmButton = {}
        )
    }
}
