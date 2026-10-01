package com.aurora.core.model

/**
 * Compatibilidade com versões antigas da Aurora.
 * O runtime atual é fornecido pelo AAR llama-android e não usa JNI próprio do projeto.
 */
class NativeBridge {
    companion object {
        const val isLinked: Boolean = true
    }
}
