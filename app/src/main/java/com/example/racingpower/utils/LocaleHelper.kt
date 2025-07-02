package com.example.racingpower.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale
import android.content.Intent // ¡IMPORTA Intent AQUÍ!

object LocaleHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val DEFAULT_LANGUAGE = "es" // Idioma predeterminado: español

    /**
     * Guarda la preferencia de idioma del usuario.
     */
    fun persist(context: Context, language: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString(KEY_APP_LANGUAGE, language)
            apply()
        }
    }

    /**
     * Obtiene el idioma persistente del usuario, o el predeterminado si no hay ninguno.
     */
    fun getPersistedLocale(context: Context): String {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_APP_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /**
     * Envuelve el contexto para aplicar la configuración de idioma.
     * Esta función es crucial para attachBaseContext.
     */
    fun wrap(context: Context, language: String): ContextWrapper {
        val configuration = context.resources.configuration
        val locale = Locale(language)
        Locale.setDefault(locale)

        // Actualizar configuración para Android N (API 24) y superior
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION") // Para suprimir la advertencia sobre configuration.locale
            configuration.locale = locale
        }

        val res = context.resources
        @Suppress("DEPRECATION") // Para suprimir la advertencia sobre updateConfiguration
        res.updateConfiguration(configuration, res.displayMetrics)

        return ContextWrapper(context)
    }

    /**
     * Aplica el idioma guardado al contexto y devuelve un ContextWrapper.
     * Útil para llamar en attachBaseContext de la Activity.
     */
    fun applyLanguage(baseContext: Context): ContextWrapper {
        val language = getPersistedLocale(baseContext)
        return wrap(baseContext, language)
    }

    /**
     * Cambia el idioma de la aplicación y recrea la actividad para aplicar los cambios.
     * Se llama desde la UI (ej. un botón).
     */
    fun changeAndRestart(context: Context, language: String) {
        persist(context, language) // Guarda la preferencia

        // Reintentar para asegurarse de que el Intent se encuentre y los flags sean Int
        val refreshIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        refreshIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(refreshIntent)

        // Opcional: Finalizar la actividad actual para que la nueva tome el control
        // Esto solo es seguro si el context es una Activity
        if (context is android.app.Activity) {
            context.finish()
        }
    }
}