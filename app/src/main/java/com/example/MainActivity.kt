package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.CampRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CampViewModel
import com.example.ui.viewmodel.CampViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import android.net.Uri
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.ui.components.EmployeeSelfRegistrationDialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.model.CampUser

sealed interface AppAuthState {
    object Checking : AppAuthState
    data class Unauthenticated(val errorMessage: String? = null) : AppAuthState
    data class Authenticated(val user: CampUser) : AppAuthState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLinkData: Uri? = intent?.data

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent(initialDeepLink = deepLinkData)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(initialDeepLink: Uri? = null) {
    val context = LocalContext.current
    val repository = remember { CampRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var showPublicSelfRegistration by remember {
        mutableStateOf(
            initialDeepLink?.let { uri ->
                (uri.scheme == "camptrack" && (uri.host == "register" || uri.path?.contains("register") == true)) ||
                (uri.scheme == "https" && (uri.host?.contains("awazelcamp.online") == true || uri.host?.contains("camptracawazel.online") == true || uri.host == "camptrack.awazel.com"))
            } ?: false
        )
    }

    var authState by remember {
        mutableStateOf<AppAuthState>(
            if (Firebase.auth.currentUser == null) AppAuthState.Unauthenticated()
            else AppAuthState.Checking
        )
    }

    // Verify existing Firebase session on initial launch
    LaunchedEffect(Unit) {
        val currentFbUser = Firebase.auth.currentUser
        if (currentFbUser == null) {
            authState = AppAuthState.Unauthenticated()
        } else {
            try {
                val verified = repository.verifyAndSyncUserSession(currentFbUser)
                authState = AppAuthState.Authenticated(verified)
            } catch (e: Exception) {
                Firebase.auth.signOut()
                try {
                    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                } catch (_: Exception) {}
                authState = AppAuthState.Unauthenticated(
                    errorMessage = e.message ?: "Pre-Login Authorization Gate: Access Denied. Your email is not registered or approved in advance by the Administrator."
                )
            }
        }
    }

    // Monitor Firebase Auth state changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null && authState is AppAuthState.Authenticated) {
                authState = AppAuthState.Unauthenticated()
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    when (val state = authState) {
        is AppAuthState.Checking -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Enforcing Pre-Login Authorization Gate...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Verifying advance Administrator registration and approval...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is AppAuthState.Unauthenticated -> {
            SignInScreen(
                initialErrorMessage = state.errorMessage,
                onAuthSuccess = { verifiedUser ->
                    authState = AppAuthState.Authenticated(verifiedUser)
                },
                onOpenSelfRegistration = {
                    showPublicSelfRegistration = true
                }
            )
        }

        is AppAuthState.Authenticated -> {
            val user = state.user
            val connectivityObserver = remember { com.example.util.NetworkConnectivityObserver(context) }
            val factory = remember(user.userId) { CampViewModelFactory(repository, connectivityObserver) }
            val viewModel: CampViewModel = viewModel(
                key = user.userId,
                factory = factory
            )
            DashboardScreen(
                viewModel = viewModel,
                onSignOut = {
                    authState = AppAuthState.Unauthenticated()
                }
            )
        }
    }

    // Public Employee Self-Registration Dialog (No login required)
    if (showPublicSelfRegistration) {
        val publicRooms by repository.roomsWithOccupantsFlow.collectAsState(initial = emptyList())
        EmployeeSelfRegistrationDialog(
            onDismiss = { showPublicSelfRegistration = false },
            onSubmit = { name, iqama, card, trade, comp, dept, phone, nat, dateIn, roomNumber, onComplete ->
                coroutineScope.launch {
                    val result = repository.registerEmployeeSelfEntry(
                        name = name,
                        iqamaNumber = iqama,
                        cardNumber = card,
                        trade = trade,
                        company = comp,
                        department = dept,
                        phone = phone,
                        nationality = nat,
                        dateIn = dateIn,
                        roomNumber = roomNumber,
                        notes = ""
                    )
                    onComplete(result)
                }
            },
            availableRooms = publicRooms.map { it.room.roomNumber }
        )
    }
}
