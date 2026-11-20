package com.alquran.noor

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alquran.noor.data.QuranRepository
import com.alquran.noor.data.local.QuranDatabase
import com.alquran.noor.data.remote.QuranApi
import com.alquran.noor.data.settings.SettingsStore
import com.alquran.noor.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** حاوية بسيطة للاعتماديات (DI يدوي بلا مكتبات إضافية). */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settings = SettingsStore(app)
    private val db = QuranDatabase.create(app)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    val repository = QuranRepository(db, QuranApi(httpClient), settings)
    val player = PlayerController(app, appScope)
}

class QuranApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as QuranApp).container

/** مصنع ViewModel عام يمرر الحاوية. */
inline fun <reified VM : ViewModel> containerFactory(
    crossinline create: CreationExtras.(AppContainer) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer { create(container()) }
}
