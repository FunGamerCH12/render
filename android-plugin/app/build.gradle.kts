import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.launchers_plugin.renderer.buildscript.RendererConfig
import com.launchers_plugin.renderer.buildscript.buildEnvs
import com.launchers_plugin.renderer.buildscript.buildJsonValue
import com.launchers_plugin.renderer.buildscript.legacyManifest
import com.launchers_plugin.renderer.buildscript.nativePath
import com.launchers_plugin.renderer.buildscript.renderer

// DSL plugin renderer Zalith/FCL (sama dengan yang dipakai plugin MobileGL).
buildscript {
    repositories {
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.github.ZalithLauncher.RendererPlugin-v2:dsl:1.0-alpha6")
    }
}

plugins {
    id("com.android.application")
}

apply(plugin = "com.launchers_plugin.renderer.dsl")

fun Project.runGit(vararg arguments: String): String? = runCatching {
    ProcessBuilder("git", *arguments)
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
        .let { process ->
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            output.takeIf { process.waitFor() == 0 && it.isNotEmpty() }
        }
}.getOrNull()

val gitShortHash = runGit("rev-parse", "--short=7", "HEAD") ?: "nogit"
val gitCommitCount = runGit("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1

// Keystore diambil dari environment (diisi dari GitHub Secrets oleh workflow).
val signingStoreFile = System.getenv("SIGNING_STORE_FILE")?.let { file(it) }
val signingStorePassword = System.getenv("SIGNING_STORE_PASSWORD")
val signingKeyAlias = System.getenv("SIGNING_KEY_ALIAS")
val signingKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")
val releaseSigningReady = signingStoreFile?.exists() == true
    && !signingStorePassword.isNullOrEmpty()
    && !signingKeyAlias.isNullOrEmpty()
    && !signingKeyPassword.isNullOrEmpty()

// Konfigurasi renderer untuk launcher yang membaca format plugin v2.
// CATATAN: rendererEGLPath memakai libEGL.so bawaan sistem (Krypton Wrapper hanya
// menyediakan sisi GL). Kalau launcher gagal memuat renderer, cek bagian ini dulu.
val pluginRendererConfig = buildJsonValue {
    renderer(
        displayName = "Krypton Wrapper (Optimizer)",
        rendererId = "opengles3",
        rendererGLPath = nativePath("libng_gl4es.so"),
        rendererEGLPath = "libEGL.so",
        dlopenLibPaths = emptyList(),
        env = buildEnvs {
            normal("LIBGL_ES", "3")
            toggleable("NGG_OPTIMIZER", "0", false, RendererConfig.MetaString("opt_disable_title"))
            toggleable("NGG_OPT_FINISH_TO_FLUSH", "0", false, RendererConfig.MetaString("opt_keep_finish_title"))
            toggleable("NGG_OPT_SKIP_GETERROR", "0", false, RendererConfig.MetaString("opt_keep_geterror_title"))
        },
        minMCVer = null,
        maxMCVer = null,
    )
}

android {
    namespace = "io.krypton.plugin"
    compileSdk = 34
    ndkVersion = "27.3.13750724"

    defaultConfig {
        applicationId = "io.krypton.plugin"
        minSdk = 26
        targetSdk = 34
        versionCode = gitCommitCount
        versionName = "1.0.$gitShortHash"
        resValue("string", "config", pluginRendererConfig)

        manifestPlaceholders.putAll(legacyManifest {
            displayName = "Krypton Wrapper (Optimizer)"
            rendererName = "KryptonWrapper"
            rendererLib = "libng_gl4es.so"
            eglLib = "libEGL.so"
            minMCVer = ""
            maxMCVer = ""
            boatEnv {
                put("LIBGL_ES", "3")
                put("POJAV_RENDERER", "opengles3")
            }
            pojavEnv {
                put("LIBGL_ES", "3")
                put("POJAV_RENDERER", "opengles3")
            }
        })
        manifestPlaceholders["appLabel"] = "Krypton Wrapper"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                // Build NG-GL4ES dari root repo. STL statis supaya tidak perlu libc++_shared.so.
                arguments += listOf("-DANDROID_STL=c++_static")
            }
        }
    }

    buildFeatures {
        resValues = true
    }

    externalNativeBuild {
        cmake {
            path = file("../../CMakeLists.txt")
            version = "3.22.1"
        }
    }

    if (releaseSigningReady) {
        signingConfigs {
            create("release") {
                storeFile = signingStoreFile
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // Launcher memuat .so langsung dari direktori native plugin, jadi harus diekstrak saat install.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

android.applicationVariants.configureEach {
    outputs.configureEach {
        (this as ApkVariantOutputImpl).outputFileName = "KryptonWrapper-plugin-$gitShortHash.apk"
    }
}
