package com.tendertracker.domain.model

data class TenderApplication(
    val id: String,
    val tenderId: String,
    val userId: String,
    val status: String, // draft, submitted
    val autoFilledFields: Map<String, AutoFilledField>,
    val pricingData: PricingData?,
    val toDoItems: List<ToDoItem>,
    val progress: Progress,
    val createdAt: String,
    val submittedAt: String?
)

data class AutoFilledField(
    val value: Any,
    val autoFilled: Boolean,
    val source: String
)

data class PricingData(
    val lineItems: List<LineItem>,
    val grandTotal: Double?
)

data class LineItem(
    val itemNo: String,
    val description: String,
    val unit: String,
    val quantity: Int,
    val unitCost: Double?,
    val total: Double?
)

data class ToDoItem(
    val id: String,
    val category: String, // missing_field, review, pricing, technical_response, document, final_check
    val priority: String, // high, medium, low
    val title: String,
    val description: String,
    val sectionReference: String? = null,
    val estimatedTime: String? = null,
    val canUseAi: Boolean = false,
    val autoFilledField: String? = null,
    val completed: Boolean,
    val completedAt: String?
)

data class Progress(
    val completed: Int,
    val total: Int,
    val percentage: Double,
    val readyToSubmit: Boolean
)

data class QualificationResult(
    val qualifies: Boolean,
    val score: Int,
    val reasons: List<String>,
    val confidence: Double,
    val tenderId: String,
    val userId: String,
    val checkedAt: String
)
