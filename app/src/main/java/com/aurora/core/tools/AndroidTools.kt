package com.aurora.core.tools

import android.content.Context
import android.os.Build

/**
 * AINDA NÃO LIGADO: nenhum código do agente chama tools nesta versão.
 * Mantido como ponto de partida para a etapa de ferramentas/permissões.
 */
interface Tool {
    val name: String
    suspend fun run(args: Map<String, Any?>): String
}

class DeviceInfoTool(private val c: Context) : Tool {
    override val name = "get_device_info"
    override suspend fun run(args: Map<String, Any?>): String =
        "${Build.MANUFACTURER} ${Build.MODEL} — Android ${Build.VERSION.RELEASE} — SDK ${Build.VERSION.SDK_INT}"
}
