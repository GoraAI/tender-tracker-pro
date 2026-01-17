package com.tendertracker.app.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tendertracker.app.ui.theme.GlassTextPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.HeatCritical
import com.tendertracker.app.ui.theme.HeatHigh
import com.tendertracker.app.ui.theme.HeatLow
import com.tendertracker.app.ui.theme.HeatMedium
import com.tendertracker.app.ui.theme.getPaymentDelayColor
import com.tendertracker.app.ui.theme.glassCard

/**
 * Region/Department payment data
 */
data class PaymentDelayData(
    val id: String,
    val name: String,
    val shortName: String,
    val avgDaysToPayment: Int,
    val sampleSize: Int = 0,
    val trend: Float = 0f  // Positive = getting worse, negative = improving
)

/**
 * Payment risk heat map
 */
@Composable
fun PaymentRiskHeatMap(
    data: List<PaymentDelayData>,
    modifier: Modifier = Modifier,
    viewMode: HeatMapViewMode = HeatMapViewMode.PROVINCE,
    selectedTenderEntity: String? = null,
    onItemClick: (PaymentDelayData) -> Unit = {},
    onViewModeChange: (HeatMapViewMode) -> Unit = {}
) {
    var selectedItem by remember { mutableStateOf<PaymentDelayData?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp)
    ) {
        // Header with view mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Payment Delay Risk",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GlassTextPrimary
            )

            Row {
                ViewModeChip(
                    label = "Province",
                    isSelected = viewMode == HeatMapViewMode.PROVINCE,
                    onClick = { onViewModeChange(HeatMapViewMode.PROVINCE) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                ViewModeChip(
                    label = "Dept",
                    isSelected = viewMode == HeatMapViewMode.DEPARTMENT,
                    onClick = { onViewModeChange(HeatMapViewMode.DEPARTMENT) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Heat map grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(data) { item ->
                HeatMapCell(
                    data = item,
                    isSelected = selectedItem == item || item.id == selectedTenderEntity,
                    onClick = {
                        selectedItem = if (selectedItem == item) null else item
                        onItemClick(item)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        HeatMapLegend()

        // Selected item details
        selectedItem?.let { item ->
            Spacer(modifier = Modifier.height(12.dp))
            PaymentDelayDetails(item)
        }
    }
}

enum class HeatMapViewMode {
    PROVINCE, DEPARTMENT
}

@Composable
private fun ViewModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Color.White.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) GlassTextPrimary else GlassTextSecondary
        )
    }
}

@Composable
private fun HeatMapCell(
    data: PaymentDelayData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cellColor = getPaymentDelayColor(data.avgDaysToPayment)

    Box(
        modifier = Modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(8.dp))
            .background(cellColor.copy(alpha = if (isSelected) 0.9f else 0.7f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data.shortName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            )
            Text(
                text = "${data.avgDaysToPayment}d",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HeatMapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = HeatLow, label = "<30d")
        Spacer(modifier = Modifier.width(8.dp))
        LegendItem(color = HeatMedium, label = "30-60d")
        Spacer(modifier = Modifier.width(8.dp))
        LegendItem(color = HeatHigh, label = "60-90d")
        Spacer(modifier = Modifier.width(8.dp))
        LegendItem(color = HeatCritical, label = ">90d")
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GlassTextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PaymentDelayDetails(data: PaymentDelayData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = data.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = GlassTextPrimary
            )
            Text(
                text = "Expected payment: ${data.avgDaysToPayment} days",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary
            )
        }

        if (data.sampleSize > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Based on ${data.sampleSize} tenders",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTextSecondary,
                    fontSize = 10.sp
                )
                if (data.trend != 0f) {
                    Text(
                        text = if (data.trend > 0) "+${data.trend.toInt()}d trend" else "${data.trend.toInt()}d trend",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (data.trend > 0) HeatCritical else HeatLow,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Cash flow impact calculator
 */
@Composable
fun CashFlowImpact(
    contractValue: Double,
    expectedDelayDays: Int,
    interestRate: Float = 0.12f,  // 12% annual
    modifier: Modifier = Modifier
) {
    val dailyRate = interestRate / 365
    val costOfDelay = contractValue * dailyRate * expectedDelayDays

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Cash Flow Impact",
                style = MaterialTheme.typography.labelMedium,
                color = GlassTextSecondary
            )
            Text(
                text = "R${formatLargeCurrency(contractValue)} tied up for ~$expectedDelayDays days",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextPrimary
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "R${formatLargeCurrency(costOfDelay)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HeatHigh
            )
            Text(
                text = "opportunity cost",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

private fun formatLargeCurrency(amount: Double): String {
    return when {
        amount >= 1_000_000 -> String.format("%.1fM", amount / 1_000_000)
        amount >= 1_000 -> String.format("%.0fK", amount / 1_000)
        else -> String.format("%.0f", amount)
    }
}
