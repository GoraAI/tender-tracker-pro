package com.tendertracker.app.ui.screens.tender

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tendertracker.app.ui.components.ScoreBadge
import com.tendertracker.app.ui.theme.GlassBorder
import com.tendertracker.app.ui.theme.GlassCardBackground
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.domain.model.Tender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenderListScreen(
    onTenderClick: (String) -> Unit,
    viewModel: TenderListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,  // Transparent for glass effect
        topBar = {
            // Glassmorphism TopAppBar
            TopAppBar(
                title = {
                    Text(
                        text = "Tenders",
                        fontWeight = FontWeight.SemiBold  // Web: fontWeight: 600
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,  // No elevation/background
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { viewModel.onSearchClick() }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = GlassPrimary  // Purple icon
                        )
                    }
                    IconButton(onClick = { viewModel.onSyncClick() }) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = GlassPrimary  // Purple icon
                        )
                    }
                }
            )
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
            state.tenders.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tenders found. Tap sync to fetch.")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(GlassEffect.screenPadding),  // 24dp
                    verticalArrangement = Arrangement.spacedBy(GlassEffect.itemSpacing)  // 16dp
                ) {
                    items(state.tenders) { tender ->
                        TenderCard(
                            tender = tender,
                            onClick = { onTenderClick(tender.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tender Card with Glassmorphism styling
 * Matches web app TenderCard component
 */
@Composable
fun TenderCard(
    tender: Tender,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Glassmorphism Card styling
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(GlassEffect.cornerRadius),  // 16dp
        colors = CardDefaults.cardColors(
            containerColor = GlassCardBackground  // rgba(30, 41, 59, 0.6)
        ),
        border = BorderStroke(
            width = GlassEffect.borderWidth,  // 1dp
            color = GlassBorder  // rgba(255, 255, 255, 0.1)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = GlassEffect.shadowElevation  // 8dp
        )
    ) {
        Column(
            modifier = Modifier.padding(GlassEffect.cardPadding)  // 24dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Reference with purple color
                    Text(
                        text = tender.reference,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassPrimary  // Purple
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Title
                    Text(
                        text = tender.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,  // Web: fontWeight: 600
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                tender.score?.let { score ->
                    ScoreBadge(score = score)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            tender.deadline?.let { deadline ->
                Text(
                    text = "Deadline: $deadline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            tender.estimatedValue?.let { value ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Est. Value: R ${"%,.0f".format(value)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
