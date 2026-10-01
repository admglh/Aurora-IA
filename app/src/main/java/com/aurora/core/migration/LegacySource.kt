package com.aurora.core.migration

import android.content.res.AssetManager
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/** Origem dos arquivos da Aurora 2.0. Retorna null quando o arquivo não existe. */
interface LegacySource {
    fun readText(name: String): String?
}

class AssetLegacySource(private val assets: AssetManager, private val dir: String) : LegacySource {
    override fun readText(name: String): String? = try {
        assets.open("$dir/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (e: FileNotFoundException) {
        null
    } catch (e: IOException) {
        // AssetManager sinaliza "não existe" com IOException/FileNotFoundException conforme a versão.
        if (assets.list(dir)?.contains(name) == true) throw e else null
    }
}

/** Para a futura importação por seleção de pasta/arquivos (Configurações > Memória). */
class DirLegacySource(private val dir: File) : LegacySource {
    override fun readText(name: String): String? =
        File(dir, name).takeIf { it.isFile }?.readText(Charsets.UTF_8)
}
