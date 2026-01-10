package com.tellraw.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tellraw.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(context.getString(R.string.help_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HelpSection(
                title = context.getString(R.string.help_about),
                content = context.getString(R.string.help_about_content)
            )

            HelpSection(
                title = context.getString(R.string.help_selector_input),
                content = context.getString(R.string.help_selector_content)
            )

            HelpSection(
                title = context.getString(R.string.help_text_formatting),
                content = context.getString(R.string.help_text_formatting_content)
            )

            HelpSection(
                title = context.getString(R.string.help_version_differences),
                content = context.getString(R.string.help_version_differences_content)
            )

            HelpSection(
                title = context.getString(R.string.help_usage_tips),
                content = context.getString(R.string.help_usage_tips_content)
            )
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}