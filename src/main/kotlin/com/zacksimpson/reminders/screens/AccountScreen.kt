package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.data.AuthException
import com.zacksimpson.reminders.data.AuthRepository
import com.zacksimpson.reminders.data.AuthState
import com.zacksimpson.reminders.data.authStateIn
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.SwipeBackContainer
import com.zacksimpson.reminders.ui.TapField
import com.zacksimpson.reminders.ui.TextEditorRequest
import com.zacksimpson.reminders.ui.TextEditorScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AccountViewModel(private val authRepo: AuthRepository) : LightViewModel<Unit>() {
    val authState = authRepo.authStateIn(viewModelScope)
    val email = MutableStateFlow("")
    val password = MutableStateFlow("")
    val error = MutableStateFlow<String?>(null)
    val isBusy = MutableStateFlow(false)

    fun setEmail(value: String) {
        email.value = value
        error.value = null
    }

    fun setPassword(value: String) {
        password.value = value
        error.value = null
    }

    fun signIn() {
        val e = email.value.trim()
        val p = password.value
        if (e.isEmpty() || p.isEmpty() || isBusy.value) return
        isBusy.value = true
        error.value = null
        viewModelScope.launch {
            try {
                authRepo.signIn(e, p)
                password.value = ""
            } catch (ex: AuthException) {
                error.value = ex.message
            } catch (ex: Exception) {
                android.util.Log.e("AccountViewModel", "Sign-in failed", ex)
                error.value = "Couldn't reach the server — check your connection."
            } finally {
                isBusy.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepo.signOut() }
    }

    override fun onCleared() {
        authRepo.close()
    }
}

/**
 * Sign in with the same email/password account used on reminders-web, for phone<->desktop
 * sync (see SYNC_PLAN.md). LightTextInputEditor has no password-masking mode, so the entry
 * screen itself shows the password in plain text while typing — unavoidable given the SDK
 * as it stands today. This screen's own row masks it as bullets once captured, so the
 * account screen itself doesn't display the password in the clear.
 */
class AccountScreen(
    sealedActivity: SealedLightActivity,
) : LightScreen<Unit, AccountViewModel>(sealedActivity) {

    override val viewModelClass: Class<AccountViewModel>
        get() = AccountViewModel::class.java

    override fun createViewModel() =
        AccountViewModel(AuthRepository(lightContext.dataStore))

    @Composable
    override fun Content() {
        RemindersTheme {
            SwipeBackContainer(onSwipeBack = { goBack(null) }) {
            val authState by viewModel.authState.collectAsState()
            val email by viewModel.email.collectAsState()
            val password by viewModel.password.collectAsState()
            val error by viewModel.error.collectAsState()
            val isBusy by viewModel.isBusy.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(null) }),
                    center = LightTopBarCenter.Text("Account"),
                    rightButton = null,
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                when (val s = authState) {
                    AuthState.Loading -> Unit

                    is AuthState.SignedIn -> SignedIn(email = s.email, onSignOut = { viewModel.signOut() })

                    AuthState.SignedOut -> {
                        TapField(
                            label = "Email",
                            value = email.ifEmpty { "Tap to enter" },
                            onClick = {
                                navigateTo(
                                    screenFactory = { TextEditorScreen(it, TextEditorRequest("Email", email)) },
                                    resultCallback = { viewModel.setEmail(it) },
                                )
                            },
                        )
                        TapField(
                            label = "Password",
                            // Own row masks the captured value — the entry screen itself
                            // still shows it in the clear while typing (see class doc).
                            value = if (password.isEmpty()) "Tap to enter" else "•".repeat(password.length),
                            onClick = {
                                navigateTo(
                                    screenFactory = { TextEditorScreen(it, TextEditorRequest("Password", password)) },
                                    resultCallback = { viewModel.setPassword(it) },
                                )
                            },
                        )
                        if (error != null) {
                            LightText(
                                text = error ?: "",
                                variant = LightTextVariant.Detail,
                                modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 0.5f.gridUnitsAsDp()),
                            )
                        }
                        if (email.isNotBlank() && password.isNotBlank()) {
                            LightText(
                                text = if (isBusy) "SIGNING IN…" else "SIGN IN",
                                variant = LightTextVariant.Button,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isBusy) { viewModel.signIn() }
                                    .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
                            )
                        }
                    }
                }
            }
            }
        }
    }

    @Composable
    private fun SignedIn(email: String, onSignOut: () -> Unit) {
        Column(modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp())) {
            LightText(text = "Signed in", variant = LightTextVariant.Detail)
            LightText(
                text = email,
                variant = LightTextVariant.Heading,
                modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp(), bottom = 1.5f.gridUnitsAsDp()),
            )
            LightText(
                text = "SIGN OUT",
                variant = LightTextVariant.Button,
                modifier = Modifier.clickable(onClick = onSignOut),
            )
        }
    }
}
