package com.therry.nortia

import android.app.Application
import android.content.Intent
import android.os.Process
import kotlin.system.exitProcess

/**
 * Instala un manejador global de excepciones no capturadas: en vez de que el
 * sistema mate el proceso sin explicación, muestra el error en [CrashActivity].
 * Temporal, para poder diagnosticar un crash sin acceso a logcat.
 */
class NortiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                val intent = Intent(this, CrashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(CrashActivity.EXTRA_ERROR, throwable.stackTraceToString())
                }
                startActivity(intent)
            } catch (_: Throwable) {
                // Si ni siquiera se puede mostrar la pantalla de error, no hay mucho más para hacer.
            }
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }
}
