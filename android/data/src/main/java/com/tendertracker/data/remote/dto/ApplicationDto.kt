package com.tendertracker.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.tendertracker.domain.model.*

data class ApplicationResponse(
    @SerializedName("id") val id: String,
    @SerializedName("tender_id") val tenderId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("status") val status: String,
    @SerializedName("auto_filled_fields") val autoFilledFields: Map<String, AutoFilledFieldDto>,
    @SerializedName("pricing_data") val pricingData: PricingDataDto?,
    @SerializedName("to_do_items") val toDoItems: List<ToDoItemDto>,
    @SerializedName("progress") val progress: ProgressDto,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("submitted_at") val submittedAt: String?
)

data class AutoFilledFieldDto(
    @SerializedName("value") val value: Any,
    @SerializedName("auto_filled") val autoFilled: Boolean,
    @SerializedName("source") val source: String
)

data class PricingDataDto(
    @SerializedName("line_items") val lineItems: List<LineItemDto>,
    @SerializedName("grand_total") val grandTotal: Double?
)

data class LineItemDto(
    @SerializedName("item_no") val itemNo: String,
    @SerializedName("description") val description: String,
    @SerializedName("unit") val unit: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_cost") val unitCost: Double?,
    @SerializedName("total") val total: Double?
)

data class ToDoItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("category") val category: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("section_reference") val sectionReference: String?,
    @SerializedName("estimated_time") val estimatedTime: String?,
    @SerializedName("can_use_ai") val canUseAi: Boolean?,
    @SerializedName("auto_filled_field") val autoFilledField: String?,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("completed_at") val completedAt: String?
)

data class ProgressDto(
    @SerializedName("completed") val completed: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("percentage") val percentage: Double,
    @SerializedName("ready_to_submit") val readyToSubmit: Boolean
)

data class QualificationResponse(
    @SerializedName("qualifies") val qualifies: Boolean,
    @SerializedName("score") val score: Int,
    @SerializedName("reasons") val reasons: List<String>,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("tender_id") val tenderId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("checked_at") val checkedAt: String
)

data class ApplicationCreateRequest(
    @SerializedName("tender_id") val tenderId: String
)

data class ApplicationUpdateRequest(
    @SerializedName("field_updates") val fieldUpdates: Map<String, Any>
)

data class PricingUpdateRequest(
    @SerializedName("pricing_data") val pricingData: Map<String, Any>
)

data class TodoUpdateRequest(
    @SerializedName("todo_id") val todoId: String,
    @SerializedName("completed") val completed: Boolean
)

// Mappers
fun ApplicationResponse.toDomain() = TenderApplication(
    id = id,
    tenderId = tenderId,
    userId = userId,
    status = status,
    autoFilledFields = autoFilledFields.mapValues { it.value.toDomain() },
    pricingData = pricingData?.toDomain(),
    toDoItems = toDoItems.map { it.toDomain() },
    progress = progress.toDomain(),
    createdAt = createdAt,
    submittedAt = submittedAt
)

fun AutoFilledFieldDto.toDomain() = AutoFilledField(
    value = value,
    autoFilled = autoFilled,
    source = source
)

fun PricingDataDto.toDomain() = PricingData(
    lineItems = lineItems.map { it.toDomain() },
    grandTotal = grandTotal
)

fun LineItemDto.toDomain() = LineItem(
    itemNo = itemNo,
    description = description,
    unit = unit,
    quantity = quantity,
    unitCost = unitCost,
    total = total
)

fun ToDoItemDto.toDomain() = ToDoItem(
    id = id,
    category = category,
    priority = priority,
    title = title,
    description = description,
    sectionReference = sectionReference,
    estimatedTime = estimatedTime,
    canUseAi = canUseAi ?: false,
    autoFilledField = autoFilledField,
    completed = completed,
    completedAt = completedAt
)

fun ProgressDto.toDomain() = Progress(
    completed = completed,
    total = total,
    percentage = percentage,
    readyToSubmit = readyToSubmit
)

fun QualificationResponse.toDomain() = QualificationResult(
    qualifies = qualifies,
    score = score,
    reasons = reasons,
    confidence = confidence,
    tenderId = tenderId,
    userId = userId,
    checkedAt = checkedAt
)
