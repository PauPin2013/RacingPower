package com.example.racingpower.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale
import android.content.Intent

// Objeto singleton que proporciona utilidades para la gestión y cambio del idioma de la aplicación.
object LocaleHelper {

    // Nombre del archivo de preferencias compartidas para almacenar la configuración de idioma.
    private const val PREFS_NAME = "app_prefs"
    // Clave para almacenar el idioma de la aplicación en las preferencias compartidas.
    private const val KEY_APP_LANGUAGE = "app_language"
    // Idioma predeterminado de la aplicación.
    private const val DEFAULT_LANGUAGE = "es"

    // Guarda el idioma seleccionado por el usuario en las preferencias compartidas.
    fun persist(context: Context, language: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString(KEY_APP_LANGUAGE, language)
            apply()
        }
    }

    // Obtiene el idioma persistente del usuario de las preferencias compartidas.
    // Si no se encuentra un idioma guardado, devuelve el idioma predeterminado.
    fun getPersistedLocale(context: Context): String {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_APP_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    // Envuelve el contexto de la aplicación para aplicar la configuración de idioma especificada.
    // Esencial para que la aplicación cargue los recursos de idioma correctos.
    fun wrap(context: Context, language: String): ContextWrapper {
        val configuration = context.resources.configuration
        val locale = Locale(language)
        Locale.setDefault(locale)

        // Establece las configuraciones regionales para Android N (API 24) y versiones posteriores.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        } else {
            // Establece la configuración regional para versiones anteriores de Android.
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }

        val res = context.resources
        // Actualiza la configuración de los recursos de la aplicación.
        @Suppress("DEPRECATION")
        res.updateConfiguration(configuration, res.displayMetrics)

        return ContextWrapper(context)
    }

    // Aplica el idioma guardado al contexto base de la aplicación.
    // Esta función se utiliza comúnmente en el método attachBaseContext de una Activity.
    fun applyLanguage(baseContext: Context): ContextWrapper {
        val language = getPersistedLocale(baseContext)
        return wrap(baseContext, language)
    }

    // Cambia el idioma de la aplicación y reinicia la Activity principal para aplicar los cambios.
    // Esta función debe ser llamada desde la interfaz de usuario (ej. al hacer clic en un botón de cambio de idioma).
    fun changeAndRestart(context: Context, language: String) {
        persist(context, language)

        // Crea un Intent para reiniciar la Activity principal de la aplicación.
        val refreshIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        // Configura las banderas del Intent para limpiar la pila de actividades y crear una nueva tarea.
        refreshIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        // Inicia la Activity con la nueva configuración de idioma.
        context.startActivity(refreshIntent)

        // Finaliza la Activity actual si el contexto proporcionado es una instancia de Activity,
        // asegurando que la nueva Activity reemplaza completamente a la anterior.
        if (context is android.app.Activity) {
            context.finish()
        }
    }
}