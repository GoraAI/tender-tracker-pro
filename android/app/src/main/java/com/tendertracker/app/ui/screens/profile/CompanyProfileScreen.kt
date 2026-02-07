package com.tendertracker.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tendertracker.app.ui.theme.GlassBorder
import com.tendertracker.app.ui.theme.GlassCardBackground
import com.tendertracker.app.ui.theme.GlassEffect
import com.tendertracker.app.ui.theme.GlassPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompanyProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: CompanyProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.onDismissError()
        }
    }

    // Show success and navigate back
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar(
                message = "Company profile saved successfully!",
                duration = SnackbarDuration.Short
            )
            viewModel.onDismissSuccess()
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
                        text = "Company Profile",
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
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = GlassPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading profile...", color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(GlassEffect.screenPadding)
            ) {
                // Profile Name
                SectionCard(title = "Profile Name") {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Profile Name") },
                        placeholder = { Text("e.g., My Construction Company") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlassPrimary,
                            unfocusedBorderColor = GlassBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

                // Capabilities
                SectionCard(title = "Capabilities") {
                    Text(
                        text = "What can your company do?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ChipInputField(
                        items = state.capabilities,
                        inputValue = state.capabilityInput,
                        onInputChange = viewModel::onCapabilityInputChange,
                        onAdd = viewModel::onCapabilityAdd,
                        onRemove = viewModel::onCapabilityRemove,
                        placeholder = "e.g., Road construction"
                    )
                }

                Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

                // Preferred Sectors
                SectionCard(title = "Preferred Sectors") {
                    Text(
                        text = "Which sectors do you prefer?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ChipInputField(
                        items = state.preferredSectors,
                        inputValue = state.sectorInput,
                        onInputChange = viewModel::onSectorInputChange,
                        onAdd = viewModel::onSectorAdd,
                        onRemove = viewModel::onSectorRemove,
                        placeholder = "e.g., Water and sanitation"
                    )
                }

                Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

                // Preferred Locations
                SectionCard(title = "Preferred Locations") {
                    Text(
                        text = "Where do you want to work?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ChipInputField(
                        items = state.preferredLocations,
                        inputValue = state.locationInput,
                        onInputChange = viewModel::onLocationInputChange,
                        onAdd = viewModel::onLocationAdd,
                        onRemove = viewModel::onLocationRemove,
                        placeholder = "e.g., Gauteng"
                    )
                }

                Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

                // Value Range
                SectionCard(title = "Value Range (ZAR)") {
                    Text(
                        text = "Minimum and maximum tender values",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.valueRangeMin,
                            onValueChange = viewModel::onValueRangeMinChange,
                            label = { Text("Minimum") },
                            placeholder = { Text("100000") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GlassPrimary,
                                unfocusedBorderColor = GlassBorder
                            )
                        )

                        OutlinedTextField(
                            value = state.valueRangeMax,
                            onValueChange = viewModel::onValueRangeMaxChange,
                            label = { Text("Maximum") },
                            placeholder = { Text("5000000") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GlassPrimary,
                                unfocusedBorderColor = GlassBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

                // Keywords
                SectionCard(title = "Keywords") {
                    Text(
                        text = "Keywords to match in tenders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ChipInputField(
                        items = state.keywords,
                        inputValue = state.keywordInput,
                        onInputChange = viewModel::onKeywordInputChange,
                        onAdd = viewModel::onKeywordAdd,
                        onRemove = viewModel::onKeywordRemove,
                        placeholder = "e.g., water"
                    )
                }

                Spacer(modifier = Modifier.height(GlassEffect.itemSpacing))

                // Scoring Weights
                SectionCard(title = "Scoring Weights") {
                    Text(
                        text = "Adjust how important each factor is",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    WeightSlider(
                        label = "Capability Match",
                        value = state.capabilityWeight.toFloat(),
                        onValueChange = viewModel::onCapabilityWeightChange
                    )

                    WeightSlider(
                        label = "Sector Match",
                        value = state.sectorWeight.toFloat(),
                        onValueChange = viewModel::onSectorWeightChange
                    )

                    WeightSlider(
                        label = "Location Match",
                        value = state.locationWeight.toFloat(),
                        onValueChange = viewModel::onLocationWeightChange
                    )

                    WeightSlider(
                        label = "Value Match",
                        value = state.valueWeight.toFloat(),
                        onValueChange = viewModel::onValueWeightChange
                    )

                    WeightSlider(
                        label = "Keyword Match",
                        value = state.keywordWeight.toFloat(),
                        onValueChange = viewModel::onKeywordWeightChange
                    )

                    val totalWeight = state.capabilityWeight + state.sectorWeight +
                            state.locationWeight + state.valueWeight + state.keywordWeight

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total: ${String.format("%.2f", totalWeight)} (should be ~1.0)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (totalWeight in 0.95..1.05) GlassPrimary else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = viewModel::onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(GlassEffect.cornerRadius)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(24.dp).height(24.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChipInputField(
    items: List<String>,
    inputValue: String,
    onInputChange: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    placeholder: String
) {
    // Display existing chips
    if (items.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                FilterChip(
                    selected = false,
                    onClick = { onRemove(item) },
                    label = { Text(item) },
                    enabled = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = GlassCardBackground,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = GlassBorder,
                        selectedBorderColor = GlassPrimary
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Input field
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = inputValue,
            onValueChange = onInputChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (inputValue.isNotBlank()) {
                        onAdd(inputValue)
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GlassPrimary,
                unfocusedBorderColor = GlassBorder
            )
        )

        IconButton(
            onClick = {
                if (inputValue.isNotBlank()) {
                    onAdd(inputValue)
                }
            },
            enabled = inputValue.isNotBlank()
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                tint = if (inputValue.isNotBlank()) GlassPrimary else GlassBorder
            )
        }
    }
}

@Composable
private fun WeightSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.bodyMedium,
                color = GlassPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 19, // 0.05 increments
            modifier = Modifier.fillMaxWidth()
        )
    }
}
