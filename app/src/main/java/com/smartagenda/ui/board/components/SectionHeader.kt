package com.smartagenda.ui.board.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartagenda.ui.theme.LocalAgendaColors

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    hint: String?,
    first: Boolean,
    modifier: Modifier = Modifier,
) {
    val extras = LocalAgendaColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (first) 12.dp else 26.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = extras.textSecondary,
            )
            if (!hint.isNullOrBlank()) {
                Text(
                    text = " · $hint",
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.textTertiary,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = extras.textTertiary,
            )
        }
    }
}