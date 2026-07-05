package io.github.soundremote

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.soundremote.data.preferences.PreferencesRepository
import io.github.soundremote.util.AppLanguage
import io.github.soundremote.util.applyAppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SoundRemoteApplication : Application() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // 启动时把 DataStore 里的语言偏好应用到 AppCompatDelegate。
        // AppCompat 自身也在 metadata service 里持久化 locale，两者通常一致；
        // 若用户手动重装或首次升级，用 DataStore 里的值兜底。
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val tag = preferencesRepository.getLanguage()
            val language = AppLanguage.fromTag(tag)
            withContext(Dispatchers.Main) { applyAppLanguage(language) }
        }
    }
}
