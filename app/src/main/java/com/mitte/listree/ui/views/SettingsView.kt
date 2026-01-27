package com.mitte.listree.ui.views

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import com.mitte.listree.MainActivity
import com.mitte.listree.R
import com.mitte.listree.ui.theme.LisTreeTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, mainActivity: MainActivity) {
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false },
            onConfirm = { locale ->
                val appLocale = if (locale.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(locale)
                }
                AppCompatDelegate.setApplicationLocales(appLocale)
                mainActivity.recreate()
                showLanguageDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LisTreeTheme.colors.topAppBarContainer,
                    titleContentColor = LisTreeTheme.colors.topAppBarTitle
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_description)
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LanguagePreference { showLanguageDialog = true }
        }
    }
}

@Composable
private fun LanguagePreference(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.language_setting_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = getCurrentLanguageDisplayName(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val languages = listOf("en" to "English", "sv" to "Svenska")
    val currentLocale = AppCompatDelegate.getApplicationLocales()
    val initialLang = if (currentLocale.isEmpty) {
        Locale.getDefault().language
    } else {
        currentLocale[0]?.language
    }
    var selectedLanguage by remember {
        mutableStateOf(languages.find { it.first == initialLang }?.first ?: "en")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_setting_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                languages.forEach { (tag, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (tag == selectedLanguage),
                                onClick = { selectedLanguage = tag },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (tag == selectedLanguage),
                            onClick = null // Recommended for accessibility with screenreaders
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedLanguage) },
                enabled = selectedLanguage.isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun getCurrentLanguageDisplayName(): String {
    val locales = AppCompatDelegate.getApplicationLocales()
    val langTag = if (locales.isEmpty) {
        Locale.getDefault().language
    } else {
        locales[0]?.language
    }

    return when (langTag) {
        "sv" -> "Svenska"
        else -> "English"
    }
}
