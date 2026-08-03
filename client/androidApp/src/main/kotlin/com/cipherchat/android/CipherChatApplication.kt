package com.cipherchat.android

import android.app.Application
import com.cipherchat.core.crypto.AndroidKeystoreSecureStorage
import com.cipherchat.core.crypto.LibsodiumSignalProtocolEngine
import com.cipherchat.core.crypto.SecureKeyStore
import com.cipherchat.core.crypto.SignalProtocolEngine
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Application class — the first entry point before any Activity or
 * Service. Responsibilities kept minimal: DI initialization, logging
 * setup, and async warm-up of libsodium (which needs initialization
 * before any crypto operation, and doing it lazily on first use risks
 * a stutter on the first message send).
 */
class CipherChatApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        setupLogging()
        startKoin {
            androidLogger()
            androidContext(this@CipherChatApplication)
            modules(cryptoModule, networkModule, repositoryModule)
        }
        warmUpLibsodium()
    }

    private fun setupLogging() {
        // Napier is CipherChat's cross-platform logging facade — on
        // Android it delegates to Logcat via DebugAntilog in debug
        // builds. In release builds, logging should be disabled or
        // directed to a crash-reporting service (never plaintext logs
        // that could capture sensitive context accidentally).
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }
    }

    private fun warmUpLibsodium() {
        // Libsodium's native library initialization is a one-time,
        // slightly expensive operation (~50ms on cold start). Doing it
        // here on the background thread during app startup means it's
        // ready by the time the user reaches the chat screen rather
        // than causing a first-message latency spike.
        appScope.launch {
            runCatching {
                com.ionspin.kotlin.crypto.LibsodiumInitializer.initialize()
                Napier.i("Libsodium initialized")
            }.onFailure {
                Napier.e("Libsodium initialization failed", it)
            }
        }
    }
}

// --- Koin module definitions ---

val cryptoModule = module {
    single<SecureKeyStore> {
        AndroidKeystoreSecureStorage(context = get())
    }
    single<SignalProtocolEngine> {
        LibsodiumSignalProtocolEngine(secureKeyStore = get())
    }
}

val networkModule = module {
    // TODO: wire CipherChatApiClient + MessageSocketSession with
    // server URL from BuildConfig / environment config file.
    // Kept as a stub until the network layer is integrated into the
    // app shell — the types are fully built in core:network.
}

val repositoryModule = module {
    // TODO: wire core:data implementations of AuthRepository,
    // ChatRepository, MessageRepository, etc., injecting their
    // dependencies (ApiClient, SocketSession, Database, CryptoEngine)
    // from the modules above. Each repository implementation is a
    // single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    // binding that makes the real implementation available to every
    // ViewModel that asks for the interface.
}
