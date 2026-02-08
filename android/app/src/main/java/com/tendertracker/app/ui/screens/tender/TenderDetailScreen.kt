package com.tendertracker.app.ui.screens.tender

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tendertracker.app.ui.components.ScoreBadge
import com.tendertracker.app.ui.components.charts.BidStrengthInsight
import com.tendertracker.app.ui.components.charts.BidStrengthRadar
import com.tendertracker.app.ui.components.charts.RadarAxis
import com.tendertracker.app.ui.theme.GlassBorder
import com.tendertracker.app.ui.theme.GlassCardBackground
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.app.ui.theme.GlassSecondary
import com.tendertracker.app.ui.screens.applications.ApplicationViewModel
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenderDetailScreen(
    tenderId: String,
    onBackClick: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToApplication: (String) -> Unit = {},
    viewModel: TenderDetailViewModel = hiltViewModel(),
    applicationViewModel: ApplicationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val applicationState by applicationViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tenderId) {
        viewModel.loadTender(tenderId)
        applicationViewModel.checkQualification(tenderId)
    }

    // Handle events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TenderDetailEvent.OpenUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                    context.startActivity(intent)
                }
                is TenderDetailEvent.DownloadPdf -> {
                    downloadPdf(context, event.url, event.filename)
                    Toast.makeText(context, "Downloading ${event.filename}...", Toast.LENGTH_SHORT).show()
                }
                is TenderDetailEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is TenderDetailEvent.NavigateToChat -> {
                    onNavigateToChat(event.tenderId)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            Column(modifier = Modifier.padding(GlassEffect.cardPadding)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                // Re-score button
                                TextButton(
                                    onClick = { viewModel.rescore() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = GlassSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Re-score", color = GlassSecondary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))
                    }

                    // Qualification Card
                    applicationState.qualificationResult?.let { qualification ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(GlassEffect.cornerRadius),
                            colors = CardDefaults.cardColors(
                                containerColor = if (qualification.qualifies)
                                    Color(0xFF4CAF50).copy(alpha = 0.15f)
                                else
                                    Color(0xFFFF9800).copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(
                                GlassEffect.borderWidth,
                                if (qualification.qualifies) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        ) {
                            Column(modifier = Modifier.padding(GlassEffect.cardPadding)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (qualification.qualifies) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (qualification.qualifies) "Auto-Fill Ready" else "Manual Application",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (qualification.qualifies) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (qualification.qualifies) "WHY Qualified:" else "WHY Not Qualified:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                qualification.reasons.forEach { reason ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "• ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = reason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (qualification.qualifies) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { onNavigateToApplication(tenderId) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GlassPrimary,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(GlassEffect.cornerRadius)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Start Application")
                                    }
                                }
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
                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick-glance chips - calculate values first (outside composables)
                    val deadlineChip: Pair<Color, String>? = tender.deadline?.let { deadlineStr ->
                        try {
                            val deadline = LocalDateTime.parse(deadlineStr, DateTimeFormatter.ISO_DATE_TIME)
                            val now = LocalDateTime.now()
                            val daysRemaining = ChronoUnit.DAYS.between(now, deadline)
                            when {
                                daysRemaining < 0 -> Color(0xFFB00020) to "Closed"
                                daysRemaining <= 3 -> Color(0xFFB00020) to "$daysRemaining days left"
                                daysRemaining <= 7 -> Color(0xFFFF9800) to "$daysRemaining days left"
                                daysRemaining <= 14 -> Color(0xFFFFC107) to "$daysRemaining days left"
                                else -> Color(0xFF4CAF50) to "$daysRemaining days left"
                            }
                        } catch (e: Exception) {
                            null // Skip chip if parsing fails
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Deadline urgency chip
                        deadlineChip?.let { (chipColor, urgencyText) ->
                            AssistChip(
                                onClick = { },
                                label = { Text(urgencyText) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = chipColor.copy(alpha = 0.2f),
                                    labelColor = chipColor
                                )
                            )
                        }

                        // Value range chip
                        tender.estimatedValue?.let { value ->
                            val valueText = when {
                                value >= 1_000_000 -> "R${String.format("%.1fM", value / 1_000_000)}"
                                value >= 1_000 -> "R${String.format("%.0fK", value / 1_000)}"
                                else -> "R${String.format("%.0f", value)}"
                            }
                            val chipColor = if (value >= 500_000) Color(0xFF4CAF50) else Color(0xFF2196F3)

                            AssistChip(
                                onClick = { },
                                label = { Text(valueText) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = chipColor.copy(alpha = 0.2f),
                                    labelColor = chipColor
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

                    // Requirements Summary (Prominent position, collapsible)
                    tender.requirementsSummary?.let { summary ->
                        var isExpanded by remember { mutableStateOf(false) }
                        val shouldTruncate = summary.length > 300

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (shouldTruncate) isExpanded = !isExpanded },
                            shape = RoundedCornerShape(GlassEffect.cornerRadius),
                            colors = CardDefaults.cardColors(containerColor = GlassCardBackground),
                            border = BorderStroke(GlassEffect.borderWidth, GlassBorder)
                        ) {
                            Column(modifier = Modifier.padding(GlassEffect.cardPadding)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Requirements Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GlassPrimary
                                    )
                                    if (shouldTruncate) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                                            tint = GlassPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isExpanded || !shouldTruncate) summary else summary.take(300) + "...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))
                    }

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

                    // PDF Buttons
                    if (!tender.pdfUrl.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.openPdf() },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, GlassPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = GlassPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View PDF", color = GlassPrimary)
                            }
                            OutlinedButton(
                                onClick = { viewModel.downloadPdf() },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, GlassSecondary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = GlassSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download", color = GlassSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))
                    }

                    // Bid Strength Radar
                    tender.score?.let { score ->
                        val radarAxes = listOf(
                            RadarAxis(
                                name = "Price",
                                value = (score * 0.9f).coerceIn(0f, 100f),
                                weight = 0.2f
                            ),
                            RadarAxis(
                                name = "Functionality",
                                value = score.toFloat(),
                                weight = 0.6f
                            ),
                            RadarAxis(
                                name = "B-BBEE",
                                value = 85f,  // Mock - would come from user profile
                                weight = 0.2f
                            ),
                            RadarAxis(
                                name = "Track Record",
                                value = (score * 0.7f).coerceIn(0f, 100f),
                                weight = 0f  // Informational
                            )
                        )

                        val insights = mutableListOf<BidStrengthInsight>()
                        if (radarAxes[2].value >= 80) {
                            insights.add(BidStrengthInsight(true, "Strong: B-BBEE Level 1"))
                        }
                        if (radarAxes[0].value >= 70) {
                            insights.add(BidStrengthInsight(true, "Competitive pricing"))
                        }
                        if (radarAxes[3].value < 60) {
                            insights.add(BidStrengthInsight(false, "Weak: Track record (limited prior contracts)"))
                        }

                        BidStrengthRadar(
                            axes = radarAxes,
                            overallScore = score,
                            tenderTitle = "Bid Assessment",
                            insights = insights
                        )
                    }

                    Spacer(modifier = Modifier.height(80.dp))
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

// Simple, battle-tested download pattern from DuckDuckGo Android browser
private fun downloadPdf(context: Context, url: String, filename: String) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // Simple filename sanitization - just replace slashes
    val cleanFilename = filename.replace("/", "_")

    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(cleanFilename)
        .setDescription("Downloading tender PDF")
        .setMimeType("application/pdf")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, cleanFilename)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)

    downloadManager.enqueue(request)
}
