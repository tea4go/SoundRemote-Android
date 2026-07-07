package io.github.soundremote.data.preferences

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    val settingsScreenPreferencesFlow: Flow<SettingsScreenPreferences>

    /**
     * Recent server addresses, from the oldest to the most recent
     */
    val serverAddressesFlow: Flow<List<String>>

    val audioCompressionFlow: Flow<Int>

    val ignoreAudioFocusFlow: Flow<Boolean>

    /** 应用语言：见 [io.github.soundremote.util.AppLanguage]（AUTO/ZH/EN） */
    val languageFlow: Flow<String>

    /** 更新源：见 [io.github.soundremote.util.UpdateSource]（gitee/github） */
    val updateSourceFlow: Flow<String>

    /** 服务器连接密码（默认 testing123） */
    val serverPasswordFlow: Flow<String>

    suspend fun setServerAddress(serverAddress: String)

    suspend fun getServerAddress(): String

    suspend fun setServerPort(value: Int)

    suspend fun getServerPort(): Int

    suspend fun setClientPort(value: Int)

    suspend fun getClientPort(): Int

    suspend fun setAudioCompression(value: Int)

    suspend fun getAudioCompression(): Int

    suspend fun setIgnoreAudioFocus(value: Boolean)

    suspend fun getIgnoreAudioFocus(): Boolean

    suspend fun setLanguage(value: String)

    suspend fun getLanguage(): String

    suspend fun setUpdateSource(value: String)

    suspend fun getUpdateSource(): String

    suspend fun setServerPassword(value: String)

    suspend fun getServerPassword(): String
}
