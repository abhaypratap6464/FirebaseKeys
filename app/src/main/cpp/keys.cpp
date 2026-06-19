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
// To regenerate:  python3 -c "print([hex(b ^ 0x7F) for b in b'YOUR_KEY'])"

// "AIzaSyDODukeJCkxFLLDBfgG1M7fr9SnAd2IzqI"
static constexpr uint8_t
kGoogleApiKey[] = {
0x3E,0x36,0x05,0x1E,0x2C,0x06,0x3B,0x30,0x3B,0x0A,0x14,0x1A,
0x35,0x3C,0x14,0x07,0x39,0x33,0x33,0x3B,0x3D,0x19,0x18,0x38,
0x4E,0x32,0x48,0x19,0x0D,0x46,0x2C,0x11,0x3E,0x1B,0x4D,0x36,
0x05,0x0E,0x36
};

// "ua-shop-45a0b"
static constexpr uint8_t
kFirebaseProjectId[] = {
0x0A,0x1E,0x52,0x0C,0x17,0x10,0x0F,0x52,0x4B,0x4A,0x1E,0x4F,
0x1D
};

// "413996980906"
static constexpr uint8_t
kFirebaseMessagingSenderId[] = {
0x4B,0x4E,0x4C,0x46,0x46,0x49,0x46,0x47,0x4F,0x46,0x4F,0x49
};

// "https://ua-shop-45a0b-default-rtdb.firebaseio.com"
static constexpr uint8_t
kFirebaseDatabaseUrl[] = {
0x17,0x0B,0x0B,0x0F,0x0C,0x45,0x50,0x50,0x0A,0x1E,0x52,0x0C,
0x17,0x10,0x0F,0x52,0x4B,0x4A,0x1E,0x4F,0x1D,0x52,0x1B,0x1A,
0x19,0x1E,0x0A,0x13,0x0B,0x52,0x0D,0x0B,0x1B,0x1D,0x51,0x19,
0x16,0x0D,0x1A,0x1D,0x1E,0x0C,0x1A,0x16,0x10,0x51,0x1C,0x10,
0x12
};

// "1:413996980906:android:7dcf70b91dd6bfd1"
static constexpr uint8_t
kFirebaseAppId[] = {
0x4E,0x45,0x4B,0x4E,0x4C,0x46,0x46,0x49,0x46,0x47,0x4F,0x46,
0x4F,0x49,0x45,0x1E,0x11,0x1B,0x0D,0x10,0x16,0x1B,0x45,0x48,
0x1B,0x1C,0x19,0x48,0x4F,0x1D,0x46,0x4E,0x1B,0x1B,0x49,0x1D,
0x19,0x1B,0x4E
};

// "key_live_bobZZ2RUuxSOMiBwOzpCscjntwlpVaac"
static constexpr uint8_t
kBranchKey[] = {
0x14,0x1A,0x06,0x20,0x13,0x16,0x09,0x1A,0x20,0x1D,0x10,0x1D,
0x25,0x25,0x4D,0x2D,0x2A,0x0A,0x07,0x2C,0x30,0x32,0x16,0x3D,
0x08,0x30,0x05,0x0F,0x3C,0x0C,0x1C,0x15,0x11,0x0B,0x08,0x13,
0x0F,0x29,0x1E,0x1E,0x1C
};

// "ua-shop-45a0b.appspot.com"
static constexpr uint8_t
kFirebaseStorageBucket[] = {
0x0A,0x1E,0x52,0x0C,0x17,0x10,0x0F,0x52,0x4B,0x4A,0x1E,0x4F,
0x1D,0x51,0x1E,0x0F,0x0F,0x0C,0x0F,0x10,0x0B,0x51,0x1C,0x10,
0x12
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

extern "C" JNIEXPORT jstring

JNICALL
Java_com_abhay_firebasekeys_NativeKeys_getBranchKey(JNIEnv *env, jobject) {
    return env->NewStringUTF(deobfuscate(kBranchKey, sizeof(kBranchKey)).c_str());
}
