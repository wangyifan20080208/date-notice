package com.smartagenda.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun SourcePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    sources: List<String>,
    modifier: Modifier = Modifier,
    label: String = "来源（群名）",
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
            trailingIcon = {
                IconButton(
                    onClick = { expanded = true },
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "选择已有来源",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            val options = (sources + listOf("手动导入")).distinct()
            options.forEach { source ->
                DropdownMenuItem(
                    text = { Text(source) },
                    onClick = {
                        onValueChange(source)
                        expanded = false
                    },
                )
            }
        }
    }
}