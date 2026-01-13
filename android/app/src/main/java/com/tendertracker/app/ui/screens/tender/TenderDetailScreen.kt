package com.tendertracker.app.ui.screens.tender

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tendertracker.app.ui.components.ScoreBadge
import com.tendertracker.app.ui.theme.GlassBorder
import com.tendertracker.app.ui.theme.GlassCardBackground
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.app.ui.theme.GlassSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenderDetailScreen(
    tenderId: String,
    onBackClick: () -> Unit,
    viewModel: TenderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(tenderId) {
        viewModel.loadTender(tenderId)
    }

    Scaffold(
        containerColor = Color.Transparent,  // Transparent for glass effect
        topBar = {
            // Glassmorphism TopAppBar
            TopAppBar(
                title = {
                    Text(
                        text = state.tender?.reference ?: "Tender",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GlassPrimary  // Purple
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // Gradient FAB matching web
            FloatingActionButton(
                onClick = { viewModel.onAskAI() },
                containerColor = GlassPrimary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp
                )
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Ask AI")
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.tender == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tender not found")
                }
            }
            else -> {
                val tender = state.tender!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(GlassEffect.screenPadding)  // 24dp
                ) {
                    // Score Card - Glassmorphism
                    tender.score?.let { score ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(GlassEffect.cornerRadius),
                            colors = CardDefaults.cardColors(containerColor = GlassCardBackground),
                            border = BorderStroke(GlassEffect.borderWidth, GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(GlassEffect.cardPadding),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Relevance Score",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    tender.scoreExplanation?.let { explanation ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                ScoreBadge(score = score, large = true)
                            }
                        }
                        Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))
                    }

                    // Title
                    Text(
                        text = tender.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Reference with purple color
                    Text(
                        text = "Reference: ${tender.reference}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassPrimary
                    )
                    Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

                    // Details Card - Glassmorphism
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(GlassEffect.cornerRadius),
                        colors = CardDefaults.cardColors(containerColor = GlassCardBackground),
                        border = BorderStroke(GlassEffect.borderWidth, GlassBorder)
                    ) {
                        Column(modifier = Modifier.padding(GlassEffect.cardPadding)) {
                            DetailRow("Source", tender.sourceId)
                            tender.deadline?.let {
                                DetailRow("Deadline", it)
                            }
                            tender.estimatedValue?.let {
                                DetailRow("Est. Value", "R ${"%,.0f".format(it)}")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

                    // Requirements Summary
                    tender.requirementsSummary?.let { summary ->
                        Text(
                            text = "Requirements Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(GlassEffect.cornerRadius),
                            colors = CardDefaults.cardColors(containerColor = GlassCardBackground),
                            border = BorderStroke(GlassEffect.borderWidth, GlassBorder)
                        ) {
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(GlassEffect.cardPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
