package com.tendertracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tendertracker.domain.model.Tender

@Entity(tableName = "tenders")
data class TenderEntity(
    @PrimaryKey val id: String,
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
) {
    fun toDomain(): Tender = Tender(
        id = id,
        title = title,
        reference = reference,
        sourceId = sourceId,
        deadline = deadline,
        estimatedValue = estimatedValue,
        requirementsSummary = requirementsSummary,
        score = score,
        scoreExplanation = scoreExplanation,
        pdfUrl = pdfUrl,
        localPdfPath = localPdfPath,
        extractedText = extractedText,
        createdAt = createdAt,
        syncedAt = syncedAt
    )

    companion object {
        fun fromDomain(tender: Tender): TenderEntity = TenderEntity(
            id = tender.id,
            title = tender.title,
            reference = tender.reference,
            sourceId = tender.sourceId,
            deadline = tender.deadline,
            estimatedValue = tender.estimatedValue,
            requirementsSummary = tender.requirementsSummary,
            score = tender.score,
            scoreExplanation = tender.scoreExplanation,
            pdfUrl = tender.pdfUrl,
            localPdfPath = tender.localPdfPath,
            extractedText = tender.extractedText,
            createdAt = tender.createdAt,
            syncedAt = tender.syncedAt
        )
    }
}
