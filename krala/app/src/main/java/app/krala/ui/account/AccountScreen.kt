package app.krala.ui.account

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.krala.R
import app.krala.data.settings.AppSettings
import app.krala.data.settings.SettingsRepository
import app.krala.data.sync.AuthRepository
import app.krala.data.sync.AuthState
import app.krala.data.sync.SyncScheduler
import app.krala.data.sync.SyncService
import app.krala.security.KeyVault
import app.krala.ui.components.*
import app.krala.ui.settings.BackTopBar
import app.krala.ui.theme.LocalHaptics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: AuthRepository,
    private val sync: SyncService,
    private val settings: SettingsRepository,
    private val vault: KeyVault,
) : ViewModel() {
    val state: StateFlow<AuthState> = auth.state
    val appSettings: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val syncing: StateFlow<Boolean> = sync.running

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    private fun run(block: suspend () -> String?) = viewModelScope.launch {
        _busy.value = true
        try {
            block()?.let { _messages.emit(it) }
        } catch (e: Exception) {
            _messages.emit(friendly(e))
        } finally {
            _busy.value = false
        }
    }

    private fun friendly(e: Exception): String {
        val m = e.message ?: e.javaClass.simpleName
        return when {
            m.contains("Invalid login", true) -> context.getString(R.string.err_invalid_login)
            m.contains("already registered", true) || m.contains("already been registered", true) -> context.getString(R.string.err_already_registered)
            m.contains("Password should", true) -> context.getString(R.string.err_weak_password)
            m.contains("Email not confirmed", true) -> context.getString(R.string.err_email_not_confirmed)
            m.contains("Anonymous sign-ins are disabled", true) -> context.getString(R.string.err_anon_disabled)
            else -> m.lineSequence().first().take(200)
        }
    }

    fun signIn(email: String, password: String) = run { auth.signIn(email, password); context.getString(R.string.signed_in) }
    fun signUp(email: String, password: String) = run {
        context.getString(
            when (auth.signUp(email, password)) {
                AuthRepository.SignUpResult.CREATED -> R.string.check_email
                AuthRepository.SignUpResult.GUEST_UPGRADED -> R.string.guest_upgraded
                AuthRepository.SignUpResult.GUEST_NEEDS_CONFIRM -> R.string.guest_upgrade_pending
            }
        )
    }
    fun guest() = run { auth.ensureGuest(); null }
    fun changePassword(p: String) = run { auth.changePassword(p); context.getString(R.string.password_changed) }
    fun signOut() = run {
        settings.setSyncEnabled(false)
        auth.signOut()
        context.getString(R.string.signed_out)
    }
    fun deleteAccount() = run {
        auth.deleteAccount()
        settings.setSyncEnabled(false)
        settings.setSyncUser(null)
        settings.resetSyncCursors()
        context.getString(R.string.account_deleted)
    }

    fun setSync(on: Boolean) = viewModelScope.launch {
        settings.setSyncEnabled(on)
        if (on) { sync.startLive(); SyncScheduler.soon(context) } else sync.stopLive()
    }

    fun syncNow() = run {
        sync.syncNow().exceptionOrNull()?.let { throw it as? Exception ?: Exception(it) }
        context.getString(R.string.synced)
    }

    fun enableSyncKeys(passphrase: String) = run {
        settings.setSyncKeys(true, vault.encrypt(passphrase))
        sync.markProvidersDirty()
        sync.syncNow()
        context.getString(R.string.sync_keys_on)
    }

    fun disableSyncKeys() = run {
        settings.setSyncKeys(false, null)
        // re-upload every provider without its key cipher, so the keys leave the cloud
        sync.markProvidersDirty()
        sync.syncNow()
        context.getString(R.string.sync_keys_off)
    }
}

private val fmt = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun AccountScreen(vm: AccountViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val s by vm.appSettings.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    var passDialog by remember { mutableStateOf(false) }
    var keysDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.account), onBack, scroll) },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection).imePadding(),
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 48.dp)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            AnimatedContent(state, label = "auth", contentKey = { it::class }) { st ->
                Column {
                    when (st) {
                        AuthState.NotConfigured -> InfoCard(Icons.Rounded.CloudOff, stringResource(R.string.cloud_not_configured), stringResource(R.string.cloud_not_configured_body))
                        AuthState.Loading -> Centered { CircularProgressIndicator(Modifier.padding(32.dp)) }
                        AuthState.SignedOut -> {
                            InfoCard(Icons.Rounded.Cloud, stringResource(R.string.signed_out_title), stringResource(R.string.signed_out_body))
                            AuthForm(busy, showGuest = true, onSignIn = vm::signIn, onSignUp = vm::signUp, onGuest = vm::guest)
                        }
                        is AuthState.Guest -> {
                            InfoCard(Icons.Rounded.PersonOutline, stringResource(R.string.guest_title), stringResource(R.string.guest_body))
                            AuthForm(busy, showGuest = false, upgrade = true, onSignIn = vm::signIn, onSignUp = vm::signUp, onGuest = {})
                        }
                        is AuthState.Account -> {
                            InfoCard(
                                Icons.Rounded.AccountCircle, st.email,
                                stringResource(if (st.confirmed) R.string.account_confirmed else R.string.account_unconfirmed),
                            )
                        }
                    }
                }
            }

            if (state is AuthState.Guest || state is AuthState.Account) {
                SectionHeader(stringResource(R.string.sync))
                SettingsCard {
                    SwitchRow(stringResource(R.string.cloud_sync), stringResource(R.string.cloud_sync_hint), Icons.Rounded.CloudSync, s.syncEnabled) { vm.setSync(it) }
                    SettingRow(
                        stringResource(R.string.sync_now),
                        when {
                            syncing -> stringResource(R.string.syncing)
                            s.lastSyncError != null -> stringResource(R.string.sync_error, s.lastSyncError!!)
                            s.lastSyncAt > 0 -> stringResource(R.string.last_synced, fmt.format(Instant.ofEpochMilli(s.lastSyncAt)))
                            else -> stringResource(R.string.never_synced)
                        },
                        Icons.Rounded.Sync,
                        onClick = if (s.syncEnabled && !syncing) ({ vm.syncNow() }) else null,
                    )
                    SwitchRow(
                        stringResource(R.string.sync_keys), stringResource(R.string.sync_keys_hint), Icons.Rounded.Key,
                        s.syncKeys, enabled = s.syncEnabled,
                    ) { on -> if (on) keysDialog = true else vm.disableSyncKeys() }
                }
            }
            if (state is AuthState.Account || state is AuthState.Guest) {
                SectionHeader(stringResource(R.string.account))
                SettingsCard {
                    if (state is AuthState.Account) SettingRow(stringResource(R.string.change_password), null, Icons.Rounded.Password, onClick = { passDialog = true })
                    SettingRow(stringResource(R.string.sign_out), stringResource(R.string.sign_out_hint), Icons.Rounded.Logout, onClick = { vm.signOut() })
                    SettingRow(stringResource(R.string.delete_account), stringResource(R.string.delete_account_hint), Icons.Rounded.PersonRemove, onClick = { deleteDialog = true })
                }
            }
        }
    }

    if (passDialog) PasswordDialog(stringResource(R.string.change_password), confirmTwice = true, onDone = { vm.changePassword(it) }, onDismiss = { passDialog = false })
    if (keysDialog) PasswordDialog(
        stringResource(R.string.sync_passphrase), body = stringResource(R.string.sync_passphrase_body), confirmTwice = true, minLength = 8,
        onDone = { vm.enableSyncKeys(it) }, onDismiss = { keysDialog = false },
    )
    if (deleteDialog) {
        var typed by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            icon = { Icon(Icons.Rounded.PersonRemove, null) },
            title = { Text(stringResource(R.string.delete_account_q)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_account_body))
                    OutlinedTextField(typed, { typed = it }, label = { Text(stringResource(R.string.type_delete)) }, singleLine = true, modifier = Modifier.padding(top = 12.dp))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { deleteDialog = false; vm.deleteAccount() },
                    enabled = typed.trim().lowercase() in setOf("delete", "удалить"),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    SettingsCard(Modifier.padding(top = 8.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun AuthForm(
    busy: Boolean,
    showGuest: Boolean,
    upgrade: Boolean = false,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onGuest: () -> Unit,
) {
    val h = LocalHaptics.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val valid = email.contains('@') && password.length >= 6
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(email, { email = it.trim() }, label = { Text(stringResource(R.string.email)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.password)) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), supportingText = { Text(stringResource(R.string.password_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        Button(onClick = { h.confirm(); onSignUp(email, password) }, enabled = valid && !busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (upgrade) R.string.upgrade_guest else R.string.create_account))
        }
        OutlinedButton(onClick = { h.click(); onSignIn(email, password) }, enabled = valid && !busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sign_in))
        }
        if (upgrade) Text(stringResource(R.string.sign_in_from_guest_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (showGuest) TextButton(onClick = onGuest, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.continue_as_guest)) }
    }
}

@Composable
private fun PasswordDialog(title: String, body: String? = null, confirmTwice: Boolean, minLength: Int = 6, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    val ok = a.length >= minLength && (!confirmTwice || a == b)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                body?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp)) }
                OutlinedTextField(a, { a = it }, label = { Text(stringResource(R.string.new_password)) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                if (confirmTwice) OutlinedTextField(
                    b, { b = it }, label = { Text(stringResource(R.string.repeat)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    isError = b.isNotEmpty() && a != b, modifier = Modifier.padding(top = 8.dp),
                )
                Text(stringResource(R.string.min_chars, minLength), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onDone(a); onDismiss() }, enabled = ok) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
