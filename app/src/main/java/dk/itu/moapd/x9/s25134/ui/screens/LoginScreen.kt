package dk.itu.moapd.x9.s25134.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, REGISTER }

@Composable
fun LoginScreen(
    isLoading: Boolean,
    onSignInWithEmail: (email: String, password: String) -> Unit,
    onRegisterWithEmail: (displayName: String, email: String, password: String) -> Unit,
    onSignInWithGoogle: (idToken: String) -> Unit,
    // Called only for Credential Manager failures (before Firebase is reached).
    // Firebase auth errors are surfaced via AuthViewModel.authError SharedFlow.
    onAuthError: (String) -> Unit,
    onContinueAsGuest: () -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fieldError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val requiredError = stringResource(R.string.error_fields_required)
    val passwordMismatchError = stringResource(R.string.error_passwords_no_match)
    val googleSignInFailedError = stringResource(R.string.error_google_sign_in_failed)
    val brandPrefix = stringResource(R.string.brand_name_prefix)
    val brandSuffix = stringResource(R.string.brand_name_suffix)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.login_horizontal_padding))
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.login_spacing_section)))

            // Branding
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.login_logo_size))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🚦", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

            Text(
                text = buildAnnotatedString {
                    append(brandPrefix)
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(brandSuffix) }
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xs)))

            Text(
                text = stringResource(R.string.subtitle_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.login_spacing_section)))

            // Mode toggle pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
            ) {
                AuthMode.entries.forEach { m ->
                    val isSelected = mode == m
                    val label = if (m == AuthMode.SIGN_IN) stringResource(R.string.label_sign_in)
                                else stringResource(R.string.label_register)
                    Button(
                        onClick = {
                            mode = m
                            fieldError = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(dimensionResource(R.dimen.button_height_toggle)),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

            // Form fields
            if (mode == AuthMode.REGISTER) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; fieldError = null },
                    label = { Text(stringResource(R.string.hint_display_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.text_field_corner_radius))
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_spacing)))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; fieldError = null },
                label = { Text(stringResource(R.string.hint_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(R.dimen.text_field_corner_radius))
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_spacing)))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; fieldError = null },
                label = { Text(stringResource(R.string.hint_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(R.dimen.text_field_corner_radius))
            )

            if (mode == AuthMode.REGISTER) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_spacing)))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; fieldError = null },
                    label = { Text(stringResource(R.string.hint_confirm_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.text_field_corner_radius))
                )
            }

            // Field error
            if (fieldError != null) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                Text(
                    text = fieldError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary action button
            Button(
                onClick = {
                    when {
                        email.isBlank() || password.isBlank() ||
                        (mode == AuthMode.REGISTER && displayName.isBlank()) -> {
                            fieldError = requiredError
                        }
                        mode == AuthMode.REGISTER && password != confirmPassword -> {
                            fieldError = passwordMismatchError
                        }
                        mode == AuthMode.SIGN_IN -> onSignInWithEmail(email.trim(), password)
                        else -> onRegisterWithEmail(displayName.trim(), email.trim(), password)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.button_height_primary)),
                shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner_radius)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    val btnLabel = if (mode == AuthMode.SIGN_IN) stringResource(R.string.btn_sign_in_email)
                                   else stringResource(R.string.btn_register)
                    Text(
                        text = btnLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

            // OR divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_spacing))
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.label_or_divider),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

            // Google Sign-In button
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val result = performGoogleSignIn(
                            context = context,
                            serverClientId = context.getString(R.string.default_web_client_id)
                        )
                        result.fold(
                            onSuccess = { idToken -> onSignInWithGoogle(idToken) },
                            onFailure = { e -> onAuthError(e.localizedMessage ?: googleSignInFailedError) }
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.button_height_primary)),
                shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner_radius)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = stringResource(R.string.btn_sign_in_google),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

            // Lets unauthenticated users browse the app without signing in
            TextButton(onClick = onContinueAsGuest) {
                Text(
                    text = stringResource(R.string.btn_continue_as_guest),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.login_spacing_section)))
        }
    }
}

// Executes the Credential Manager Google Sign-In flow and returns a Result wrapping the ID token.
// Must be called from a coroutine with an Activity-derived Context (required by CredentialManager).
private suspend fun performGoogleSignIn(
    context: Context,
    serverClientId: String
): Result<String> {
    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(serverClientId)
        .setFilterByAuthorizedAccounts(false)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    return try {
        val result = credentialManager.getCredential(context = context, request = request)
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
        Result.success(googleIdTokenCredential.idToken)
    } catch (e: GetCredentialException) {
        Result.failure(e)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenSignInPreview() {
    X9ComposeTheme(darkTheme = true) {
        LoginScreen(
            isLoading = false,
            onSignInWithEmail = { _, _ -> },
            onRegisterWithEmail = { _, _, _ -> },
            onSignInWithGoogle = { _ -> },
            onAuthError = { _ -> },
            onContinueAsGuest = {}
        )
    }
}
