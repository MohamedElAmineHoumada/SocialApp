package com.Groupe15.SocialApp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.Groupe15.SocialApp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppScreen()
        }
    }
}

@Composable
fun AppScreen() {

    var showRegister by remember {
        mutableStateOf(false)
    }

    if(showRegister){
        RegisterScreen(
            onBackToLogin = {
                showRegister = false
            }
        )
    }else{
        LoginScreen(
            onGoRegister = {
                showRegister = true
            }
        )
    }
}

@Composable
fun LoginScreen(
    onGoRegister: () -> Unit
) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "SocialApp Login",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {

                auth.signInWithEmailAndPassword(email,password)
                    .addOnSuccessListener {

                        Toast.makeText(
                            context,
                            "Login Success",
                            Toast.LENGTH_LONG
                        ).show()

                    }
                    .addOnFailureListener {

                        Toast.makeText(
                            context,
                            it.message,
                            Toast.LENGTH_LONG
                        ).show()

                    }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                onGoRegister()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go To Register")
        }
    }
}

@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit
) {

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Register",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {

                auth.createUserWithEmailAndPassword(email,password)
                    .addOnSuccessListener {

                        val uid = auth.currentUser!!.uid

                        val user = User(
                            uid,
                            username,
                            email
                        )

                        db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener {

                                Toast.makeText(
                                    context,
                                    "Registration Success",
                                    Toast.LENGTH_LONG
                                ).show()

                                onBackToLogin()

                            }

                    }
                    .addOnFailureListener {

                        Toast.makeText(
                            context,
                            it.message,
                            Toast.LENGTH_LONG
                        ).show()

                    }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                onBackToLogin()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back To Login")
        }
    }
}