package com.tendertracker.app.ui.screens.applications

import com.tendertracker.domain.model.AutoFilledField
import com.tendertracker.domain.model.Progress
import com.tendertracker.domain.model.QualificationResult
import com.tendertracker.domain.model.TenderApplication
import com.tendertracker.domain.model.ToDoItem
import com.tendertracker.domain.repository.ApplicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationViewModelTest {

    private lateinit var viewModel: ApplicationViewModel
    private lateinit var repository: ApplicationRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        viewModel = ApplicationViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.uiState.first()

        assertFalse(state.isLoadingQualification)
        assertFalse(state.isCreating)
        assertFalse(state.isLoading)
        assertFalse(state.isUpdating)
        assertFalse(state.isSubmitting)
        assertNull(state.qualificationResult)
        assertNull(state.application)
        assertFalse(state.createSuccess)
        assertFalse(state.submitSuccess)
        assertNull(state.error)
        assertNull(state.qualificationError)
    }

    @Test
    fun `checkQualification success updates state`() = runTest {
        val tenderId = "tender-123"
        val qualificationResult = QualificationResult(
            qualifies = true,
            score = 85,
            reasons = listOf(
                "Score: 85/100 (threshold: 70)",
                "Capabilities: 3 matches (minimum: 2)",
                "Value: R500,000 (in range: R100,000 - R5,000,000)",
                "Deadline: 14 days (minimum: 7 days)"
            )
        )

        whenever(repository.checkQualification(tenderId))
            .thenReturn(Result.success(qualificationResult))

        viewModel.checkQualification(tenderId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        assertFalse(state.isLoadingQualification)
        assertNotNull(state.qualificationResult)
        assertTrue(state.qualificationResult!!.qualifies)
        assertEquals(85, state.qualificationResult!!.score)
        assertEquals(4, state.qualificationResult!!.reasons.size)
        assertNull(state.qualificationError)
    }

    @Test
    fun `checkQualification failure updates error state`() = runTest {
        val tenderId = "tender-123"
        val errorMessage = "Network error"

        whenever(repository.checkQualification(tenderId))
            .thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.checkQualification(tenderId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        assertFalse(state.isLoadingQualification)
        assertNull(state.qualificationResult)
        assertNotNull(state.qualificationError)
        assertTrue(state.qualificationError!!.contains(errorMessage))
    }

    @Test
    fun `createApplication success updates state with application`() = runTest {
        val tenderId = "tender-123"
        val application = createMockApplication()

        whenever(repository.createApplication(tenderId))
            .thenReturn(Result.success(application))

        viewModel.createApplication(tenderId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        assertFalse(state.isCreating)
        assertNotNull(state.application)
        assertEquals(application.id, state.application!!.id)
        assertTrue(state.createSuccess)
        assertNull(state.error)
    }

    @Test
    fun `createApplication failure updates error state`() = runTest {
        val tenderId = "tender-123"
        val errorMessage = "Failed to create application"

        whenever(repository.createApplication(tenderId))
            .thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.createApplication(tenderId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        assertFalse(state.isCreating)
        assertNull(state.application)
        assertFalse(state.createSuccess)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains(errorMessage))
    }

    @Test
    fun `loadApplication success loads application into state`() = runTest {
        val applicationId = "app-123"
        val application = createMockApplication(id = applicationId)

        whenever(repository.getApplication(applicationId))
            .thenReturn(Result.success(application))

        viewModel.loadApplication(applicationId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        assertFalse(state.isLoading)
        assertNotNull(state.application)
        assertEquals(applicationId, state.application!!.id)
        assertNull(state.error)
    }

    @Test
    fun `loadApplication failure shows error`() = runTest {
        val applicationId = "app-123"
        val errorMessage = "Application not found"

        whenever(repository.getApplication(applicationId))
            .thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.loadApplication(applicationId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        assertFalse(state.isLoading)
        assertNull(state.application)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains(errorMessage))
    }

    @Test
    fun `updateField success updates application in state`() = runTest {
        val applicationId = "app-123"
        val initialApplication = createMockApplication(id = applicationId)
        val updatedApplication = initialApplication.copy(
            autoFilledFields = initialApplication.autoFilledFields + mapOf(
                "contact_email" to AutoFilledField(
                    value = "test@example.com",
                    autoFilled = false,
                    source = "User Input"
                )
            )
        )

        // Set initial state
        whenever(repository.createApplication(any()))
            .thenReturn(Result.success(initialApplication))
        viewModel.createApplication("tender-123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Update field
        whenever(repository.updateFields(applicationId, mapOf("contact_email" to "test@example.com")))
            .thenReturn(Result.success(updatedApplication))

        viewModel.updateField("contact_email", "test@example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        assertFalse(state.isUpdating)
        assertNotNull(state.application)
        assertTrue(state.application!!.autoFilledFields.containsKey("contact_email"))
        assertEquals("test@example.com", state.application!!.autoFilledFields["contact_email"]!!.value)
    }

    @Test
    fun `toggleTodoStatus success updates application`() = runTest {
        val applicationId = "app-123"
        val initialApplication = createMockApplication(id = applicationId)
        val updatedTodos = initialApplication.toDoItems.map {
            if (it.id == "todo-1") it.copy(completed = true) else it
        }
        val updatedApplication = initialApplication.copy(
            toDoItems = updatedTodos,
            progress = Progress(
                total = updatedTodos.size,
                completed = updatedTodos.count { it.completed },
                percentage = (updatedTodos.count { it.completed } * 100) / updatedTodos.size,
                readyToSubmit = updatedTodos.all { it.completed }
            )
        )

        // Set initial state
        whenever(repository.createApplication(any()))
            .thenReturn(Result.success(initialApplication))
        viewModel.createApplication("tender-123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle to-do
        whenever(repository.updateTodoStatus(applicationId, "todo-1", true))
            .thenReturn(Result.success(updatedApplication))

        viewModel.toggleTodoStatus("todo-1", true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        assertNotNull(state.application)
        val todo = state.application!!.toDoItems.find { it.id == "todo-1" }
        assertNotNull(todo)
        assertTrue(todo!!.completed)
        assertEquals(1, state.application!!.progress.completed)
    }

    @Test
    fun `submitApplication success updates status and shows success`() = runTest {
        val applicationId = "app-123"
        val initialApplication = createMockApplication(
            id = applicationId,
            progress = Progress(
                total = 1,
                completed = 1,
                percentage = 100,
                readyToSubmit = true
            )
        )
        val submittedApplication = initialApplication.copy(status = "submitted")

        // Set initial state
        whenever(repository.createApplication(any()))
            .thenReturn(Result.success(initialApplication))
        viewModel.createApplication("tender-123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Submit
        whenever(repository.submitApplication(applicationId))
            .thenReturn(Result.success(submittedApplication))

        viewModel.submitApplication()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        assertFalse(state.isSubmitting)
        assertNotNull(state.application)
        assertEquals("submitted", state.application!!.status)
        assertTrue(state.submitSuccess)
        assertNull(state.error)
    }

    @Test
    fun `submitApplication failure shows error`() = runTest {
        val applicationId = "app-123"
        val initialApplication = createMockApplication(id = applicationId)
        val errorMessage = "Submission failed"

        // Set initial state
        whenever(repository.createApplication(any()))
            .thenReturn(Result.success(initialApplication))
        viewModel.createApplication("tender-123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Submit (fail)
        whenever(repository.submitApplication(applicationId))
            .thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.submitApplication()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        assertFalse(state.isSubmitting)
        assertFalse(state.submitSuccess)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains(errorMessage))
    }

    @Test
    fun `dismissError clears error state`() = runTest {
        val tenderId = "tender-123"

        whenever(repository.createApplication(tenderId))
            .thenReturn(Result.failure(Exception("Error")))

        viewModel.createApplication(tenderId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error is set
        var state = viewModel.uiState.first()
        assertNotNull(state.error)

        // Dismiss error
        viewModel.dismissError()
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.first()
        assertNull(state.error)
        assertNull(state.qualificationError)
    }

    @Test
    fun `dismissSuccess clears success flags`() = runTest {
        val tenderId = "tender-123"
        val application = createMockApplication()

        whenever(repository.createApplication(tenderId))
            .thenReturn(Result.success(application))

        viewModel.createApplication(tenderId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify success is set
        var state = viewModel.uiState.first()
        assertTrue(state.createSuccess)

        // Dismiss success
        viewModel.dismissSuccess()
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.first()
        assertFalse(state.createSuccess)
        assertFalse(state.submitSuccess)
    }

    @Test
    fun `updateField does nothing when application is null`() = runTest {
        // Don't create application first
        viewModel.updateField("field", "value")
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not call repository
        verify(repository, org.mockito.kotlin.never()).updateFields(any(), any())
    }

    @Test
    fun `updatePricing success updates pricing data`() = runTest {
        val applicationId = "app-123"
        val initialApplication = createMockApplication(id = applicationId)
        val pricingData = mapOf(
            "items" to listOf(
                mapOf("name" to "Item 1", "price" to 1000)
            ),
            "total" to 1000
        )
        val updatedApplication = initialApplication.copy(pricingData = pricingData)

        // Set initial state
        whenever(repository.createApplication(any()))
            .thenReturn(Result.success(initialApplication))
        viewModel.createApplication("tender-123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Update pricing
        whenever(repository.updatePricing(applicationId, pricingData))
            .thenReturn(Result.success(updatedApplication))

        viewModel.updatePricing(pricingData)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()

        assertFalse(state.isUpdating)
        assertNotNull(state.application)
        assertNotNull(state.application!!.pricingData)
        assertEquals(1000, state.application!!.pricingData!!["total"])
    }

    // Helper function to create mock application
    private fun createMockApplication(
        id: String = "app-123",
        progress: Progress = Progress(
            total = 3,
            completed = 0,
            percentage = 0,
            readyToSubmit = false
        )
    ): TenderApplication {
        return TenderApplication(
            id = id,
            tenderId = "tender-123",
            status = "draft",
            autoFilledFields = mapOf(
                "company_name" to AutoFilledField(
                    value = "Test Company Ltd",
                    autoFilled = true,
                    source = "Company Profile"
                )
            ),
            pricingData = null,
            toDoItems = listOf(
                ToDoItem(
                    id = "todo-1",
                    category = "pricing",
                    priority = "high",
                    title = "Provide pricing",
                    description = "Add pricing details",
                    completed = false,
                    sectionReference = null
                ),
                ToDoItem(
                    id = "todo-2",
                    category = "review",
                    priority = "medium",
                    title = "Review company overview",
                    description = "Verify auto-filled content",
                    completed = false,
                    sectionReference = null
                ),
                ToDoItem(
                    id = "todo-3",
                    category = "final_check",
                    priority = "low",
                    title = "Final review",
                    description = "Review before submission",
                    completed = false,
                    sectionReference = null
                )
            ),
            progress = progress
        )
    }
}
