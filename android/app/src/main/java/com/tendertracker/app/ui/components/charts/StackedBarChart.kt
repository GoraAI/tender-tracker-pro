package com.tendertracker.app.ui.components.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tendertracker.app.ui.theme.CostCompliance
import com.tendertracker.app.ui.theme.CostOpportunity
import com.tendertracker.app.ui.theme.CostPlatform
import com.tendertracker.app.ui.theme.CostPreparation
import com.tendertracker.app.ui.theme.CostSubmission
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.StatusSuccess
import com.tendertracker.app.ui.theme.getCostCategoryColor
import com.tendertracker.app.ui.theme.glassCard

/**
 * Cost category data
 */
data class CostCategory(
    val name: String,
    val amount: Double,
    val percentage: Float = 0f,
    val items: List<CostItem> = emptyList()
)

/**
 * Individual cost item
 */
data class CostItem(
    val id: String,
    val description: String,
    val amount: Double,
    val date: String = "",
    val tenderId: String? = null
)

/**
 * Cost per successful bid calculator
 */
@Composable
fun CostPerBidCalculator(
    categories: List<CostCategory>,
    totalSpent: Double,
    tendersWon: Int,
    modifier: Modifier = Modifier,
    periodLabel: String = "12 months",
    onAddCost: () -> Unit = {},
    onCategoryClick: (CostCategory) -> Unit = {}
) {
    var expandedCategory by remember { mutableStateOf<CostCategory?>(null) }
    val costPerWin = if (tendersWon > 0) totalSpent / tendersWon else 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cost Per Successful Bid",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GlassTextPrimary
            )
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Summary metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CostMetric(
                label = "Total Spent",
                value = "R${formatCostValue(totalSpent)}"
            )
            CostMetric(
                label = "Won",
                value = tendersWon.toString()
            )
            CostMetric(
                label = "Cost/Win",
                value = "R${formatCostValue(costPerWin)}",
                highlight = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stacked bar
        CostStackedBar(
            categories = categories,
            totalAmount = totalSpent,
            expandedCategory = expandedCategory,
            onCategoryClick = { category ->
                expandedCategory = if (expandedCategory == category) null else category
                onCategoryClick(category)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category legend
        CostLegend(categories = categories)

        // Expanded category details
        expandedCategory?.let { category ->
            Spacer(modifier = Modifier.height(12.dp))
            CostCategoryBreakdown(category = category)
        }

        // Break-even calculation
        Spacer(modifier = Modifier.height(16.dp))
        BreakEvenCalculation(costPerWin = costPerWin)

        // Add cost button
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = onAddCost,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Cost")
        }
    }
}

@Composable
private fun CostMetric(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) StatusSuccess else GlassTextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GlassTextSecondary
        )
    }
}

@Composable
private fun CostStackedBar(
    categories: List<CostCategory>,
    totalAmount: Double,
    expandedCategory: CostCategory?,
    onCategoryClick: (CostCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        categories.forEach { category ->
            val fraction = if (totalAmount > 0) (category.amount / totalAmount).toFloat() else 0f
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(durationMillis = 600),
                label = "cost_fraction_${category.name}"
            )
            val categoryColor = getCostCategoryColor(category.name)
            val isExpanded = expandedCategory == category

            if (animatedFraction > 0) {
                Box(
                    modifier = Modifier
                        .weight(animatedFraction.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(
                            if (isExpanded) categoryColor else categoryColor.copy(alpha = 0.7f)
                        )
                        .clickable { onCategoryClick(category) },
                    contentAlignment = Alignment.Center
                ) {
                    if (animatedFraction > 0.15f) {
                        Text(
                            text = "${(fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CostLegend(categories: List<CostCategory>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        categories.forEach { category ->
            CostLegendItem(
                name = category.name,
                amount = category.amount,
                color = getCostCategoryColor(category.name)
            )
        }
    }
}

@Composable
private fun CostLegendItem(
    name: String,
    amount: Double,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = name.take(8),
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary,
                fontSize = 9.sp
            )
        }
        Text(
            text = "R${formatCostValue(amount)}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun CostCategoryBreakdown(category: CostCategory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Text(
            text = "Breakdown: ${category.name}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = GlassTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (category.items.isEmpty()) {
            Text(
                text = "No detailed items recorded",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary
            )
        } else {
            category.items.take(5).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "R${formatCostValue(item.amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = getCostCategoryColor(category.name)
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakEvenCalculation(costPerWin: Double) {
    val minContractForBreakEven = costPerWin / 0.025  // Assuming 2.5% margin

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StatusSuccess.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Break-even Analysis",
                style = MaterialTheme.typography.labelMedium,
                color = GlassTextSecondary
            )
            Text(
                text = "Win 1 tender @ R${formatCostValue(minContractForBreakEven)} with 2.5% margin",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextPrimary
            )
        }
    }
}

/**
 * Compact cost summary for dashboard
 */
@Composable
fun CostSummary(
    totalSpent: Double,
    costPerWin: Double,
    tendersWon: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cost Analytics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = GlassTextPrimary
            )
            Text(
                text = "R${formatCostValue(totalSpent)} spent on $tendersWon wins",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "R${formatCostValue(costPerWin)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StatusSuccess
            )
            Text(
                text = "per win",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary
            )
        }
    }
}

private fun formatCostValue(amount: Double): String {
    return when {
        amount >= 1_000_000 -> String.format("%.1fM", amount / 1_000_000)
        amount >= 1_000 -> String.format("%.0fK", amount / 1_000)
        else -> String.format("%.0f", amount)
    }
}
