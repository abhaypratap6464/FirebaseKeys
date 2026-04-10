#include <jni.h>
#include <string>
#include <cstdlib>   // abort()

// XOR key used to obfuscate all secrets stored in this binary.
// The obfuscated byte arrays below were produced by XORing every
// character of the original string with 0x7F, so no plaintext
// appears in the compiled .so file.
static constexpr uint8_t
XOR_KEY = 0x7F;

static std::string deobfuscate(const uint8_t *data, size_t len) {
    std::string result(len, '\0');
    for (size_t i = 0; i < len; i++) {
        result[i] = static_cast<char>(data[i] ^ XOR_KEY);
    }
    return result;
}

// ── Package guard ─────────────────────────────────────────────────────────────
// "com.abhay.firebasekeys" XOR'd with 0x7F
static constexpr uint8_t
kExpectedPackage[] = {
0x1C,0x10,0x12,0x51,0x1E,0x1D,0x17,0x1E,0x06,0x51,
0x19,0x16,0x0D,0x1A,0x1D,0x1E,0x0C,0x1A,0x14,0x1A,
0x06,0x0C
};

// Called from JNI_OnLoad — crashes the process if the calling app's package
// name does not match the expected value. This runs before any key is exposed.
static void enforcePackage(JNIEnv *env) {
    // Retrieve the running application via ActivityThread.currentApplication()
    jclass activityThread = env->FindClass("android/app/ActivityThread");
    jmethodID currentApp = env->GetStaticMethodID(
            activityThread, "currentApplication", "()Landroid/app/Application;");
    jobject app = env->CallStaticObjectMethod(activityThread, currentApp);

    // Call Context.getPackageName()
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID getPkg = env->GetMethodID(
            contextClass, "getPackageName", "()Ljava/lang/String;");
    auto pkg = (jstring) env->CallObjectMethod(app, getPkg);

    const char *pkgCStr = env->GetStringUTFChars(pkg, nullptr);
    std::string expected = deobfuscate(kExpectedPackage, sizeof(kExpectedPackage));
    bool valid = (expected == pkgCStr);
    env->ReleaseStringUTFChars(pkg, pkgCStr);

    if (!valid) {
        // Package mismatch — abort immediately, no keys are returned
        abort();
    }
}

// ── Obfuscated secrets ────────────────────────────────────────────────────────
// Each array is the original string XOR'd byte-by-byte with 0x7F.
// To regenerate:  python3 -c "print([b ^ 0x7F for b in b'YOUR_KEY'])"

// "AIzaSyDmPDjH-kAnPIZy8TYbK_qfu1AAaXN6TaU"
static constexpr uint8_t
kGoogleApiKey[] = {
0x3E,0x36,0x05,0x1E,0x2C,0x06,0x3B,0x12,0x2F,0x3B,0x15,0x37,
0x52,0x14,0x3E,0x11,0x2F,0x36,0x25,0x06,0x47,0x2B,0x26,0x1D,
0x34,0x20,0x0E,0x19,0x0A,0x4E,0x3E,0x3E,0x1E,0x27,0x31,0x49,
0x2B,0x1E,0x2A
};

// "maadhaarplus-192715"
static constexpr uint8_t
kFirebaseProjectId[] = {
0x12,0x1E,0x1E,0x1B,0x17,0x1E,0x1E,0x0D,0x0F,0x13,0x0A,0x0C,
0x52,0x4E,0x46,0x4D,0x48,0x4E,0x4A
};

// "366473305126"
static constexpr uint8_t
kFirebaseMessagingSenderId[] = {
0x4C,0x49,0x49,0x4B,0x48,0x4C,0x4C,0x4F,0x4A,0x4E,0x4D,0x49
};

// "https://maadhaarplus-192715.firebaseio.com"
static constexpr uint8_t
kFirebaseDatabaseUrl[] = {
0x17,0x0B,0x0B,0x0F,0x0C,0x45,0x50,0x50,0x12,0x1E,0x1E,0x1B,
0x17,0x1E,0x1E,0x0D,0x0F,0x13,0x0A,0x0C,0x52,0x4E,0x46,0x4D,
0x48,0x4E,0x4A,0x51,0x19,0x16,0x0D,0x1A,0x1D,0x1E,0x0C,0x1A,
0x16,0x10,0x51,0x1C,0x10,0x12
};

// "1:366473305126:android:579e4acddd167a26355ae7"
static constexpr uint8_t
kFirebaseAppId[] = {
0x4E,0x45,0x4C,0x49,0x49,0x4B,0x48,0x4C,0x4C,0x4F,0x4A,0x4E,
0x4D,0x49,0x45,0x1E,0x11,0x1B,0x0D,0x10,0x16,0x1B,0x45,0x4A,
0x48,0x46,0x1A,0x4B,0x1E,0x1C,0x1B,0x1B,0x1B,0x4E,0x49,0x48,
0x1E,0x4D,0x49,0x4C,0x4A,0x4A,0x1E,0x1A,0x48
};

// "maadhaarplus-192715.appspot.com"
static constexpr uint8_t
kFirebaseStorageBucket[] = {
0x12,0x1E,0x1E,0x1B,0x17,0x1E,0x1E,0x0D,0x0F,0x13,0x0A,0x0C,
0x52,0x4E,0x46,0x4D,0x48,0x4E,0x4A,0x51,0x1E,0x0F,0x0F,0x0C,
0x0F,0x10,0x0B,0x51,0x1C,0x10,0x12
};

// ── Library entry point ───────────────────────────────────────────────────────
// JNI_OnLoad is called the instant System.loadLibrary("keys") executes.
// Package verification happens here — before any external function can be called.
JNIEXPORT jint
JNI_OnLoad(JavaVM
* vm, void* /*reserved*/) {
JNIEnv *env;
if (vm->GetEnv(reinterpret_cast
<void **>(&env), JNI_VERSION_1_6
) != JNI_OK) {
return
JNI_ERR;
}
enforcePackage(env);
return
JNI_VERSION_1_6;
}

// ── JNI exports ───────────────────────────────────────────────────────────────

extern "C" JNIEXPORT jstring

JNICALL
Java_com_abhay_firebasekeys_NativeKeys_getGoogleApiKey(JNIEnv *env, jobject) {
    return env->NewStringUTF(deobfuscate(kGoogleApiKey, sizeof(kGoogleApiKey)).c_str());
}

extern "C" JNIEXPORT jstring

JNICALL
Java_com_abhay_firebasekeys_NativeKeys_getFirebaseProjectId(JNIEnv *env, jobject) {
    return env->NewStringUTF(deobfuscate(kFirebaseProjectId, sizeof(kFirebaseProjectId)).c_str());
}

extern "C" JNIEXPORT jstring

JNICALL
Java_com_abhay_firebasekeys_NativeKeys_getFirebaseMessagingSenderId(JNIEnv *env, jobject) {
    return env->NewStringUTF(
            deobfuscate(kFirebaseMessagingSenderId, sizeof(kFirebaseMessagingSenderId)).c_str());
}

extern "C" JNIEXPORT jstring

JNICALL
Java_com_abhay_firebasekeys_NativeKeys_getFirebaseDatabaseUrl(JNIEnv *env, jobject) {
    return env->NewStringUTF(
            deobfuscate(kFirebaseDatabaseUrl, sizeof(kFirebaseDatabaseUrl)).c_str());
}

extern "C" JNIEXPORT jstring

JNICALL
Java_com_abhay_firebasekeys_NativeKeys_getFirebaseAppId(JNIEnv *env, jobject) {
    return env->NewStringUTF(deobfuscate(kFirebaseAppId, sizeof(kFirebaseAppId)).c_str());
}

extern "C" JNIEXPORT jstring

JNICALL
Java_com_abhay_firebasekeys_NativeKeys_getFirebaseStorageBucket(JNIEnv *env, jobject) {
    return env->NewStringUTF(
            deobfuscate(kFirebaseStorageBucket, sizeof(kFirebaseStorageBucket)).c_str());
}
