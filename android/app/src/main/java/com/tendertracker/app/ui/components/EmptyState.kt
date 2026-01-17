package com.tendertracker.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary

/**
 * Empty state component for screens with no data.
 * Shows a friendly message and optional call-to-action.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GlassEffect.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = GlassTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = GlassTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = GlassTextSecondary,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = actionLabel)
                }
            }
        }
    }
}

/**
 * Pre-configured empty states for different screens.
 */
object EmptyStates {
    @Composable
    fun Compliance(
        onAddRegistration: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.CheckCircle,
            title = "No Compliance Registrations",
            message = "Add your compliance documents (CSD, CIDB, B-BBEE, Tax Clearance) to track expiry dates and stay tender-ready.",
            actionLabel = "Add Registration",
            onAction = onAddRegistration,
            modifier = modifier
        )
    }

    @Composable
    fun Pipeline(
        onAddTender: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.Timeline,
            title = "No Active Tenders",
            message = "Add tenders you're working on to track deadlines, manage phases, and stay organized.",
            actionLabel = "Add Tender",
            onAction = onAddTender,
            modifier = modifier
        )
    }

    @Composable
    fun Funnel(
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.Analytics,
            title = "No Tender History",
            message = "Your opportunity funnel will appear here once you start tracking tender outcomes. Add tenders to your pipeline and record results to see your win rate.",
            modifier = modifier
        )
    }

    @Composable
    fun Costs(
        onAddCost: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.Assignment,
            title = "No Cost Data",
            message = "Track your tender preparation costs to calculate your cost-per-win and optimize your bidding strategy.",
            actionLabel = "Add Cost",
            onAction = onAddCost,
            modifier = modifier
        )
    }
}
