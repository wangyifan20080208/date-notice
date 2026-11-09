package com.smartagenda.ui.board.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartagenda.ui.theme.LocalAgendaColors

@Composable
fun SourceFilterRow(
    sources: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalAgendaColors.current
    val shape = RoundedCornerShape(percent = 50)

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "__all__") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("全部") },
                shape = shape,
                colors = filterChipColors(
                    selected = selected == null,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == null,
                    borderColor = extras.hairline,
                    selectedBorderColor = Color.Transparent,
                ),
            )
        }

        items(sources, key = { it }) { source ->
            FilterChip(
                selected = selected == source,
                onClick = { onSelect(source) },
                label = { Text(source, maxLines = 1) },
                shape = shape,
                colors = filterChipColors(selected = selected == source),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == source,
                    borderColor = extras.hairline,
                    selectedBorderColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun filterChipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = LocalAgendaColors.current.textSecondary,
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
)