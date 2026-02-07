package com.tendertracker.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tendertracker.app.ui.theme.GlassBorder
import com.tendertracker.app.ui.theme.GlassCardBackground
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.app.ui.theme.GlassSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onModelManagerClick: () -> Unit,
    onCompanyProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,  // Glass effect background
        topBar = {
            // Glassmorphism TopAppBar
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(GlassEffect.screenPadding)  // 24dp
        ) {
            // Model Manager - Glassmorphism Card
            SettingsCard(
                title = "AI Model",
                subtitle = state.modelStatus,
                icon = Icons.Default.Memory,
                onClick = onModelManagerClick
            )

            Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

            // Company Profile - Glassmorphism Card
            SettingsCard(
                title = "Company Profile",
                subtitle = "Manage scoring criteria",
                icon = Icons.Default.Business,
                onClick = onCompanyProfileClick
            )

            Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

            // Sync Settings - Glassmorphism Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlassEffect.cornerRadius),
                colors = CardDefaults.cardColors(containerColor = GlassCardBackground),
                border = BorderStroke(GlassEffect.borderWidth, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(GlassEffect.cardPadding)) {
                    Text(
                        text = "Sync Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsToggle(
                        title = "Auto Sync",
                        subtitle = "Sync tenders automatically",
                        icon = Icons.Default.Sync,
                        checked = state.autoSyncEnabled,
                        onCheckedChange = { viewModel.onAutoSyncChange(it) }
                    )

                    SettingsToggle(
                        title = "WiFi Only",
                        subtitle = "Only sync on WiFi",
                        icon = Icons.Default.Cloud,
                        checked = state.wifiOnlySync,
                        onCheckedChange = { viewModel.onWifiOnlyChange(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

            // Notifications - Glassmorphism Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlassEffect.cornerRadius),
                colors = CardDefaults.cardColors(containerColor = GlassCardBackground),
                border = BorderStroke(GlassEffect.borderWidth, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(GlassEffect.cardPadding)) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsToggle(
                        title = "Push Notifications",
                        subtitle = "Receive alerts for new tenders",
                        icon = Icons.Default.Notifications,
                        checked = state.notificationsEnabled,
                        onCheckedChange = { viewModel.onNotificationsChange(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

            // App Info - Glassmorphism Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlassEffect.cornerRadius),
                colors = CardDefaults.cardColors(containerColor = GlassCardBackground),
                border = BorderStroke(GlassEffect.borderWidth, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(GlassEffect.cardPadding)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tender Tracker Pro v1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "On-device AI powered by Gemma 3n",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

            // Logout Button - Glassmorphism Card
            SettingsCard(
                title = "Sign Out",
                subtitle = "Sign out of your account",
                icon = Icons.Default.ExitToApp,
                onClick = onLogoutClick
            )
        }
    }
}

/**
 * Settings Card with Glassmorphism styling
 */
@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(GlassEffect.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = GlassCardBackground),
        border = BorderStroke(GlassEffect.borderWidth, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GlassEffect.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GlassPrimary  // Purple
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = GlassPrimary  // Purple
            )
        }
    }
}

/**
 * Settings Toggle with Glassmorphism styling
 * Purple switch when enabled
 */
@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GlassPrimary  // Purple
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GlassPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = GlassBorder
            )
        )
    }
}
