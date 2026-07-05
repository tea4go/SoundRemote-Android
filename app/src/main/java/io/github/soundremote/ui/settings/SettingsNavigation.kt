package io.github.soundremote.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.soundremote.BuildConfig
import io.github.soundremote.R
import io.github.soundremote.util.UpdateInfo
import io.github.soundremote.util.UpdateService
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
object SettingsRoute

fun NavController.navigateToSettings() {
    navigate(SettingsRoute)
}

/** 更新流程 UI 状态机：闲置 / 检查中 / 无更新 / 有更新 / 下载中 / 失败 */
private sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object None : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val info: UpdateInfo) : UpdateUiState
    data object Failed : UpdateUiState
}

fun NavGraphBuilder.settingsScreen(
    onNavigateUp: () -> Unit,
) {
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = hiltViewModel()
        val settings by viewModel.settings.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var updateState: UpdateUiState by remember { mutableStateOf(UpdateUiState.Idle) }

        SettingsScreen(
            settings = settings,
            onSetServerPort = viewModel::setServerPort,
            onSetClientPort = viewModel::setClientPort,
            onSetAudioCompression = viewModel::setAudioCompression,
            onSetIgnoreAudioFocus = viewModel::setIgnoreAudioFocus,
            onSetLanguage = viewModel::setLanguage,
            onSetUpdateSource = viewModel::setUpdateSource,
            onCheckUpdate = {
                updateState = UpdateUiState.Checking
                scope.launch {
                    val info = runCatching {
                        UpdateService.checkForUpdate(settings.updateSource)
                    }.onFailure { Timber.w(it, "checkForUpdate failed") }.getOrNull()

                    updateState = when {
                        info == null -> UpdateUiState.Failed
                        info.hasUpdate -> UpdateUiState.Available(info)
                        else -> UpdateUiState.None
                    }
                }
            },
            onNavigateUp = onNavigateUp,
        )

        UpdateDialogs(
            state = updateState,
            onDismiss = { updateState = UpdateUiState.Idle },
            onUpdateNow = { info ->
                updateState = UpdateUiState.Downloading(info)
                scope.launch {
                    val result = runCatching {
                        val apk = UpdateService.downloadApk(context, info.downloadUrl, info.versionCode)
                        UpdateService.openInstaller(context, apk)
                    }.onFailure { Timber.w(it, "downloadApk failed") }
                    updateState = if (result.isSuccess) UpdateUiState.Idle else UpdateUiState.Failed
                }
            },
        )
    }
}

/**
 * 根据 [UpdateUiState] 渲染对应的对话框；Idle 不显示任何 UI。
 */
@Composable
private fun UpdateDialogs(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onUpdateNow: (UpdateInfo) -> Unit,
) {
    when (state) {
        UpdateUiState.Idle -> {}
        UpdateUiState.Checking -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.update_checking)) },
            text = { CircularProgressIndicator() },
            confirmButton = {},
        )
        UpdateUiState.None -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.update_none_title)) },
            text = {
                Text(
                    stringResource(R.string.update_none_message).format(BuildConfig.VERSION_NAME)
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
        UpdateUiState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.update_check_failed)) },
            text = { Text(stringResource(R.string.update_download_failed)) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
        is UpdateUiState.Available -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.update_available_title)) },
            text = {
                Text(
                    stringResource(R.string.update_available_message).format(
                        state.info.versionName,
                        BuildConfig.VERSION_NAME,
                        state.info.releaseNotes.ifBlank { "-" },
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { onUpdateNow(state.info) }) {
                    Text(stringResource(R.string.update_download))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_later))
                }
            },
        )
        is UpdateUiState.Downloading -> AlertDialog(
            onDismissRequest = {},   // 下载中不允许关闭
            title = { Text(stringResource(R.string.update_downloading)) },
            text = { CircularProgressIndicator() },
            confirmButton = {},
        )
    }
}
