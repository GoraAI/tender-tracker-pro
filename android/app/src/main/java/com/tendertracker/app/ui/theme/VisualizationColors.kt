package com.tendertracker.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Visualization Color Semantics
 * Based on OpenSpec Tender Visualisation Skill proposal
 */

// =============================================================================
// URGENCY / RISK COLORS
// =============================================================================
val UrgencyCritical = Color(0xFFDC2626)    // Red - Risk, urgent, failure
val UrgencyWarning = Color(0xFFEA580C)     // Orange - Attention needed
val UrgencyCaution = Color(0xFFCA8A04)     // Yellow - Review, partial
val UrgencySafe = Color(0xFF16A34A)        // Green - Valid, complete, won
val UrgencyInfo = Color(0xFF2563EB)        // Blue - Interactive, new
val UrgencyNeutral = Color(0xFF6B7280)     // Gray - Disabled, historical

// =============================================================================
// COMPLIANCE STATUS COLORS
// =============================================================================
val ComplianceValid = Color(0xFF16A34A)     // Green - >90 days
val ComplianceExpiring = Color(0xFFCA8A04)  // Yellow - 30-90 days
val ComplianceUrgent = Color(0xFFEA580C)    // Orange - 7-30 days
val ComplianceExpired = Color(0xFFDC2626)   // Red - <7 days or expired

// =============================================================================
// FUNNEL STAGE COLORS
// =============================================================================
val FunnelDiscovered = Color(0xFF8B5CF6)    // Purple - Initial discovery
val FunnelQualified = Color(0xFF3B82F6)     // Blue - Passed qualification
val FunnelSubmitted = Color(0xFFF59E0B)     // Amber - Submitted
val FunnelAwarded = Color(0xFF16A34A)       // Green - Won
val FunnelDropped = Color(0xFFEF4444)       // Red - Lost/Dropped

// =============================================================================
// COST CATEGORY COLORS
// =============================================================================
val CostPlatform = Color(0xFF8B5CF6)        // Purple - Platform/subscription
val CostCompliance = Color(0xFF3B82F6)      // Blue - Compliance/certifications
val CostPreparation = Color(0xFFF59E0B)     // Amber - Preparation time
val CostSubmission = Color(0xFFEC4899)      // Pink - Submission costs
val CostOpportunity = Color(0xFF6B7280)     // Gray - Opportunity cost

// =============================================================================
// RADAR CHART COLORS
// =============================================================================
val RadarFill = Color(0xFF8B5CF6).copy(alpha = 0.3f)
val RadarStroke = Color(0xFF8B5CF6)
val RadarWinnerFill = Color(0xFF16A34A).copy(alpha = 0.2f)
val RadarWinnerStroke = Color(0xFF16A34A)
val RadarGridLine = Color.White.copy(alpha = 0.1f)
val RadarAxisLabel = Color(0xFF94A3B8)

// =============================================================================
// HEAT MAP COLORS (Payment Delay)
// =============================================================================
val HeatLow = Color(0xFF16A34A)       // <30 days
val HeatMedium = Color(0xFFF59E0B)    // 30-60 days
val HeatHigh = Color(0xFFEA580C)      // 60-90 days
val HeatCritical = Color(0xFFDC2626)  // >90 days

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * Get urgency color based on days remaining
 */
fun getUrgencyColor(daysRemaining: Int): Color = when {
    daysRemaining < 0 -> UrgencyCritical    // Expired
    daysRemaining < 7 -> UrgencyCritical    // Critical
    daysRemaining < 30 -> UrgencyWarning    // Warning
    daysRemaining < 90 -> UrgencyCaution    // Caution
    else -> UrgencySafe                      // Safe
}

/**
 * Get compliance gauge color
 */
fun getComplianceColor(daysRemaining: Int): Color = when {
    daysRemaining < 0 -> ComplianceExpired
    daysRemaining < 7 -> ComplianceExpired
    daysRemaining < 30 -> ComplianceUrgent
    daysRemaining < 90 -> ComplianceExpiring
    else -> ComplianceValid
}

/**
 * Get payment delay heat color
 */
fun getPaymentDelayColor(days: Int): Color = when {
    days < 30 -> HeatLow
    days < 60 -> HeatMedium
    days < 90 -> HeatHigh
    else -> HeatCritical
}

/**
 * Get funnel stage color
 */
fun getFunnelStageColor(stage: String): Color = when (stage.lowercase()) {
    "discovered" -> FunnelDiscovered
    "qualified" -> FunnelQualified
    "submitted" -> FunnelSubmitted
    "awarded" -> FunnelAwarded
    else -> FunnelDropped
}

/**
 * Get cost category color
 */
fun getCostCategoryColor(category: String): Color = when (category.lowercase()) {
    "platform" -> CostPlatform
    "compliance" -> CostCompliance
    "preparation" -> CostPreparation
    "submission" -> CostSubmission
    "opportunity" -> CostOpportunity
    else -> UrgencyNeutral
}
