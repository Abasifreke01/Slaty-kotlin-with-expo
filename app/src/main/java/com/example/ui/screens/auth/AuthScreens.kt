package com.example.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.UserRole
import com.example.ui.screens.student.defaultTextFieldColors
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    onLoginAsStudent: () -> Unit,
    onLoginAsTutor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(id = R.drawable.slaty_login_logo),
            contentDescription = "Slaty Logo",
            modifier = Modifier
                .width(260.dp)
                .height(80.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Welcome to Slaty",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose how you want to continue",
            color = Color(0xFFAAAAAA),
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onLoginAsStudent,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("welcome_login_student_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SlatyPrimary)
        ) {
            Text(
                text = "Login as Student",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onLoginAsTutor,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("welcome_login_tutor_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(2.dp, SlatyPrimary)
        ) {
            Text(
                text = "Login as Tutor",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "By continuing, you agree to Slaty's",
                color = Color(0xFF777777),
                fontSize = 12.sp
            )
            Row(modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    text = "Terms of Service",
                    color = SlatyMint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = " and ",
                    color = Color(0xFF777777),
                    fontSize = 12.sp
                )
                Text(
                    text = "Privacy Policy",
                    color = SlatyMint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    role: UserRole,
    onBack: () -> Unit,
    onLoginSuccess: (email: String, role: UserRole) -> Unit,
    onGoogleSignIn: () -> Unit,
    onRegisterSuccess: (email: String, pass: String, name: String, role: UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(if (role == UserRole.TUTOR) "Dr. Emeka Johnson" else "Tobi Bakare") }
    var emailOrUsername by remember { mutableStateOf(if (role == UserRole.TUTOR) "tutor@slaty.edu.ng" else "student@slaty.edu.ng") }
    var password by remember { mutableStateOf("slatypass123") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSignUpMode) "Create ${role.displayName} Account" else "${role.displayName} Sign In", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlatyDarkBackground)
            )
        },
        containerColor = SlatyDarkBackground
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.slaty_login_logo),
                contentDescription = "Slaty",
                modifier = Modifier
                    .width(180.dp)
                    .height(54.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (role == UserRole.TUTOR) Color(0xFF1B3B36) else Color(0xFF272142)
            ) {
                Text(
                    text = "${if (isSignUpMode) "Registering" else "Signing in"} as ${role.displayName}",
                    color = if (role == UserRole.TUTOR) SlatyMint else SlatyLavender,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isSignUpMode) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SlatyMint) },
                    modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
                    colors = defaultTextFieldColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = emailOrUsername,
                onValueChange = { emailOrUsername = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SlatyMint) },
                modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                colors = defaultTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SlatyMint) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                colors = defaultTextFieldColors()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isSignUpMode) {
                        onRegisterSuccess(emailOrUsername.trim(), password.trim(), name.trim(), role)
                    } else {
                        onLoginSuccess(emailOrUsername.trim(), role)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_submit_login_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SlatyPrimary)
            ) {
                Text(if (isSignUpMode) "Create Account" else "Sign In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Google Sign-In with Credential Manager
            OutlinedButton(
                onClick = onGoogleSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_google_sign_in_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF443D60))
            ) {
                Text("Continue with Google", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Toggle Mode
            TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
                Text(
                    text = if (isSignUpMode) "Already have an account? Sign In" else "Don't have an account? Sign Up",
                    color = SlatyMint,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick 1-tap demo logins
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onLoginSuccess("david.student@slaty.edu.ng", UserRole.STUDENT) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlatyMint)
                ) {
                    Text("Demo Student", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onLoginSuccess("emmanuel.tutor@slaty.edu.ng", UserRole.TUTOR) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlatyLavender)
                ) {
                    Text("Demo Tutor", fontSize = 11.sp)
                }
            }
        }
    }
}
