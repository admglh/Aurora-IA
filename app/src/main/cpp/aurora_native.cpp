#include <jni.h>
#include <android/log.h>
#include <string>

#define TAG "AuroraNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

// Runtime nativo preparado e compilado pelo app. A inferência llama.cpp ainda não está
// vendorada neste repositório, portanto nativeCreate retorna 0 deliberadamente.
extern "C" JNIEXPORT jlong JNICALL
Java_com_aurora_core_model_NativeBridge_nativeCreate(JNIEnv*, jobject, jstring, jint, jint) {
    LOGI("Aurora native bridge loaded; llama.cpp runtime not bundled");
    return 0;
}
extern "C" JNIEXPORT void JNICALL Java_com_aurora_core_model_NativeBridge_nativeCancel(JNIEnv*, jobject, jlong) {}
extern "C" JNIEXPORT void JNICALL Java_com_aurora_core_model_NativeBridge_nativeDestroy(JNIEnv*, jobject, jlong) {}
extern "C" JNIEXPORT jstring JNICALL
Java_com_aurora_core_model_NativeBridge_nativeStatus(JNIEnv* env, jobject, jlong) {
    return env->NewStringUTF("runtime-linked:not-configured");
}
