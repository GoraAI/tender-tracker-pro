package com.tendertracker.domain.model

data class Tender(
    val id: String,
    val title: String,
    val reference: String,
    val sourceId: String,
    val deadline: String?,
    val estimatedValue: Double?,
    val requirementsSummary: String?,
    val score: Int?,
    val scoreExplanation: String?,
    val pdfUrl: String,
    val localPdfPath: String?,
    val extractedText: String?,
    val createdAt: Long,
    val syncedAt: Long?
)
