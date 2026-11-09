package com.smartagenda.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartagenda.ui.theme.LocalAgendaColors

enum class BannerTone { Neutral, Warning }

@Composable
fun InlineBanner(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Neutral,
) {
    val extras = LocalAgendaColors.current
    val isWarning = tone == BannerTone.Warning
    val shape = RoundedCornerShape(10.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = if (isWarning) extras.amberSoft else extras.elevated,
        border = BorderStroke(
            1.dp,
            if (isWarning) extras.amberLine else extras.hairline,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isWarning) extras.amber else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.textSecondary,
                )
            }
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isWarning) extras.amber else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}