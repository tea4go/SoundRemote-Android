package io.github.soundremote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.soundremote.data.preferences.PreferencesRepository
import io.github.soundremote.util.AppLanguage
import io.github.soundremote.util.UpdateSource
import io.github.soundremote.util.applyAppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    val settings: StateFlow<SettingsUIState> = combine(
        preferencesRepository.settingsScreenPreferencesFlow,
        preferencesRepository.languageFlow,
        preferencesRepository.updateSourceFlow,
    ) { prefs, languageTag, updateSourceTag ->
        SettingsUIState(
            serverPort = prefs.serverPort,
            clientPort = prefs.clientPort,
            audioCompression = prefs.audioCompression,
            ignoreAudioFocus = prefs.ignoreAudioFocus,
            language = AppLanguage.fromTag(languageTag),
            updateSource = UpdateSource.fromTag(updateSourceTag),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUIState(),
    )

    fun setServerPort(value: Int) {
        viewModelScope.launch { preferencesRepository.setServerPort(value) }
    }

    fun setClientPort(value: Int) {
        viewModelScope.launch { preferencesRepository.setClientPort(value) }
    }

    fun setAudioCompression(value: Int) {
        viewModelScope.launch { preferencesRepository.setAudioCompression(value) }
    }

    fun setIgnoreAudioFocus(value: Boolean) {
        viewModelScope.launch { preferencesRepository.setIgnoreAudioFocus(value) }
    }

    fun setLanguage(value: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(value.tag)
            // 立即应用；Compose UI 会通过 Activity 的 Configuration change 自动重组
            applyAppLanguage(value)
        }
    }

    fun setUpdateSource(value: UpdateSource) {
        viewModelScope.launch { preferencesRepository.setUpdateSource(value.tag) }
    }
}

data class SettingsUIState(
    val serverPort: Int = 0,
    val clientPort: Int = 0,
    val audioCompression: Int = 0,
    val ignoreAudioFocus: Boolean = false,
    val language: AppLanguage = AppLanguage.AUTO,
    val updateSource: UpdateSource = UpdateSource.GITEE,
)
