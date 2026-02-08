package com.tendertracker.app.ui.screens.applications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tendertracker.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationPreviewScreen(
    tenderId: String,
    applicationId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ApplicationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load or create application
    LaunchedEffect(Unit) {
        if (applicationId != null) {
            viewModel.loadApplication(applicationId)
        } else {
            viewModel.createApplication(tenderId)
        }
    }

    // Show error snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissError()
        }
    }

    // Show success and navigate back
    LaunchedEffect(state.submitSuccess) {
        if (state.submitSuccess) {
            snackbarHostState.showSnackbar(
                message = "Application submitted successfully!",
                duration = SnackbarDuration.Short
            )
            viewModel.dismissSuccess()
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Application Preview",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        if (state.isLoading || state.isCreating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GlassPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (state.isCreating) "Generating application..." else "Loading...",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        } else if (state.application != null) {
            ApplicationContent(
                application = state.application!!,
                isSubmitting = state.isSubmitting,
                onFieldUpdate = viewModel::updateField,
                onTodoToggle = viewModel::toggleTodoStatus,
                onSubmit = viewModel::submitApplication,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ApplicationContent(
    application: com.tendertracker.domain.model.TenderApplication,
    isSubmitting: Boolean,
    onFieldUpdate: (String, Any) -> Unit,
    onTodoToggle: (String, Boolean) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(GlassEffect.screenPadding)
    ) {
        // Progress Card
        ProgressCard(application.progress)

        Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

        // Auto-Filled Fields
        SectionCard(title = "Auto-Filled Information") {
            Text(
                text = "${application.autoFilledFields.size} fields auto-filled",
                style = MaterialTheme.typography.bodyMedium,
                color = GlassPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            application.autoFilledFields.forEach { (name, field) ->
                FieldItem(
                    name = name,
                    value = field.value.toString(),
                    autoFilled = field.autoFilled,
                    source = field.source
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

        // To-Do List
        ToDoListCard(
            todos = application.toDoItems,
            onTodoToggle = onTodoToggle
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isSubmitting && application.progress.readyToSubmit,
            colors = ButtonDefaults.buttonColors(
                containerColor = GlassPrimary,
                contentColor = Color.White,
                disabledContainerColor = GlassBorder
            ),
            shape = RoundedCornerShape(GlassEffect.cornerRadius)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Icon(
                    if (application.progress.readyToSubmit) Icons.Default.CheckCircle else Icons.Default.Download,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (application.progress.readyToSubmit) "Submit Application" else "Complete To-Do Items First"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProgressCard(progress: com.tendertracker.domain.model.Progress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlassEffect.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = GlassCardBackground),
        border = BorderStroke(GlassEffect.borderWidth, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(GlassEffect.cardPadding)) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (progress.percentage / 100).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = GlassPrimary,
                trackColor = GlassBorder,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${progress.completed}/${progress.total} tasks complete (${progress.percentage.toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlassEffect.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = GlassCardBackground),
        border = BorderStroke(GlassEffect.borderWidth, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(GlassEffect.cardPadding)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FieldItem(
    name: String,
    value: String,
    autoFilled: Boolean,
    source: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name.replace("_", " ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (autoFilled) {
                Text(
                    text = "✓ Auto-filled",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassPrimary
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (autoFilled) {
            Text(
                text = "Source: $source",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ToDoListCard(
    todos: List<com.tendertracker.domain.model.ToDoItem>,
    onTodoToggle: (String, Boolean) -> Unit
) {
    SectionCard(title = "To-Do List") {
        if (todos.isEmpty()) {
            Text(
                text = "No tasks remaining",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Group by priority
            val high = todos.filter { it.priority == "high" }
            val medium = todos.filter { it.priority == "medium" }
            val low = todos.filter { it.priority == "low" }

            if (high.isNotEmpty()) {
                Text(
                    text = "🔴 High Priority",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                high.forEach { todo ->
                    ToDoItem(todo = todo, onToggle = onTodoToggle)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (medium.isNotEmpty()) {
                Text(
                    text = "🟡 Medium Priority",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                medium.forEach { todo ->
                    ToDoItem(todo = todo, onToggle = onTodoToggle)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (low.isNotEmpty()) {
                Text(
                    text = "🟢 Low Priority",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                low.forEach { todo ->
                    ToDoItem(todo = todo, onToggle = onTodoToggle)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ToDoItem(
    todo: com.tendertracker.domain.model.ToDoItem,
    onToggle: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = todo.completed,
            onCheckedChange = { onToggle(todo.id, it) },
            colors = CheckboxDefaults.colors(
                checkedColor = GlassPrimary,
                uncheckedColor = GlassBorder
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = todo.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            todo.sectionReference?.let { ref ->
                Text(
                    text = ref,
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassPrimary
                )
            }
        }
    }
}
