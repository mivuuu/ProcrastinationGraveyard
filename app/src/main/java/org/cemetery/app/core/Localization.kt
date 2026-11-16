package org.cemetery.app.core
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale
fun localizedContext(context: Context, selection: String): Context {
    val system = android.content.res.Resources.getSystem().configuration.locales[0]
    val language = if (selection == "system") system.language.takeIf { it == "ru" || it == "en" } ?: "en" else selection
    val config = Configuration(context.resources.configuration)
    config.setLocales(LocaleList(Locale.forLanguageTag(language)))
    return context.createConfigurationContext(config)
}
