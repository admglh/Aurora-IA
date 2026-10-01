package com.aurora

import android.app.Application
import android.util.Log
import com.aurora.core.agent.AuroraAgent
import com.aurora.core.agent.MemoryRetriever
import com.aurora.core.agent.PersonaProvider
import com.aurora.core.data.ChatRepository
import com.aurora.core.database.AuroraDatabase
import com.aurora.core.migration.AssetLegacySource
import com.aurora.core.migration.LegacyMigration
import com.aurora.core.model.ModelManager
import com.aurora.core.settings.AuroraSettings
import com.aurora.core.settings.SecretStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Composição manual das dependências (sem framework de DI). Vive enquanto o processo viver. */
class AppContainer(private val app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = AuroraSettings(app)
    val secrets = SecretStore(app)
    val db: AuroraDatabase = AuroraDatabase.get(app)
    val chat = ChatRepository(db)
    val models = ModelManager(app, settings, secrets, appScope)
    val agent = AuroraAgent(PersonaProvider(app), MemoryRetriever(db.memories()), models)

    /** Importação da 2.0 + primeira avaliação de modelos. A UI aguarda este Job antes de escolher a conversa inicial. */
    val startup: Job = appScope.launch {
        runLegacyImportIfNeeded()
        models.refresh()
    }

    private suspend fun runLegacyImportIfNeeded() {
        if (settings.legacyImportDone) return
        val result = LegacyMigration(db, AssetLegacySource(app.assets, "legacy/data")).run()
        settings.lastImportReport = result.summary()
        Log.i(TAG, "Importação Aurora 2.0: ${result.summary()}")
        // Só marca como concluído quando a origem realmente existia e foi processada.
        if (result.fatalError == null && result.filesFound > 0) settings.legacyImportDone = true
    }

    private companion object {
        const val TAG = "AppContainer"
    }
}
