package com.smartagenda.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartagenda.AppContainer
import com.smartagenda.data.prefs.AiProvider
import com.smartagenda.ingest.NotificationAccess
import com.smartagenda.reminder.ExactAlarmPermission
import com.smartagenda.ui.theme.LocalAgendaColors

@Composable
fun SettingsRoute(container: AppContainer) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(container.settingsStore, container.aiParseEngine)
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(state = state, viewModel = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    val extras = LocalAgendaColors.current
    val context = LocalContext.current

    var notificationAccess by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    var postNotifications by remember { mutableStateOf(NotificationAccess.hasPostPermission(context)) }
    var exactAlarm by remember { mutableStateOf(ExactAlarmPermission.isGranted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccess = NotificationAccess.isGranted(context)
                postNotifications = NotificationAccess.hasPostPermission(context)
                exactAlarm = ExactAlarmPermission.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> postNotifications = granted }

    var providerMenuExpanded by remember { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var ignoredInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 48.dp),
    ) {
        item {
            Text(
                text = "设置",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "所有数据只保存在这台设备上。",
                style = MaterialTheme.typography.bodyMedium,
                color = extras.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
        }

        item {
            SettingsSection(title = "大模型") {
                ExposedDropdownMenuBox(
                    expanded = providerMenuExpanded,
                    onExpandedChange = { providerMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = state.provider.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("服务商") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(providerMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = providerMenuExpanded,
                        onDismissRequest = { providerMenuExpanded = false },
                    ) {
                        AiProvider.entries.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.label) },
                                onClick = {
                                    viewModel.selectProvider(provider)
                                    providerMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.model,
                    onValueChange = viewModel::updateModel,
                    label = { Text("模型名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.isCustom) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.baseUrl,
                        onValueChange = viewModel::updateBaseUrl,
                        label = { Text("Base URL") },
                        supportingText = { Text("需要以 /v1/ 结尾，例如 https://example.com/v1/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = viewModel::updateApiKey,
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (apiKeyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Text(if (apiKeyVisible) "隐藏" else "显示")
                        }
                    },
                    supportingText = {
                        Text("Key 会先用 Android Keystore 加密，再写入本机存储。")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "自动解析群消息",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "关闭后只记录消息，不调用大模型。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extras.textSecondary,
                        )
                    }
                    Switch(checked = state.enabled, onCheckedChange = viewModel::setEnabled)
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = viewModel::testConnection,
                        enabled = !state.isTesting,
                    ) {
                        Text(if (state.isTesting) "测试中…" else "测试连接")
                    }
                    Button(
                        onClick = { viewModel.save() },
                        enabled = !state.isSaving && state.initialized,
                    ) {
                        Text(if (state.isSaving) "保存中…" else "保存配置")
                    }
                }

                val message = state.message
                if (message != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.messageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }

        item {
            SettingsSection(title = "权限") {
                PermissionRow(
                    title = "通知使用权",
                    description = "读取微信、QQ 的通知内容，是本应用的核心前提。",
                    granted = notificationAccess,
                    actionLabel = "去开启",
                    onAction = {
                        runCatching { context.startActivity(NotificationAccess.settingsIntent()) }
                    },
                )
                PermissionRow(
                    title = "通知提醒",
                    description = "允许本应用发出到点提醒。",
                    granted = postNotifications,
                    actionLabel = "授权",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
                PermissionRow(
                    title = "精确闹钟",
                    description = "Android 12 起需要单独授权，才能准点提醒。",
                    granted = exactAlarm,
                    actionLabel = "授权",
                    onAction = {
                        val intent = ExactAlarmPermission.requestIntent(context)
                        if (intent != null) {
                            runCatching { context.startActivity(intent) }
                        }
                    },
                )
            }
        }

        item {
            SettingsSection(title = "忽略的来源") {
                Text(
                    text = "来自这些群或联系人的消息不会调用大模型。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = ignoredInput,
                        onValueChange = { ignoredInput = it },
                        label = { Text("群名或联系人名") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.addIgnoredSource(ignoredInput)
                            ignoredInput = ""
                        },
                    ) {
                        Text("添加")
                    }
                }
                if (state.ignoredSources.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    state.ignoredSources.forEach { source ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(source) },
                            trailingIcon = {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "移除 $source",
                                    modifier = Modifier.width(16.dp),
                                )
                            },
                            modifier = Modifier.padding(end = 6.dp, bottom = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val extras = LocalAgendaColors.current

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = extras.textTertiary,
            modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
        )
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val extras = LocalAgendaColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = extras.textSecondary,
            )
        }
        if (granted) {
            Text(
                text = "已开启",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp),
            )
        } else {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}