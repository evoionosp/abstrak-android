package org.evoionosp.abstrak.presentation.settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.connection.model.ServerRequestHeader
import org.evoionosp.abstrak.presentation.R

internal sealed interface HeaderDialogState {
    data object Hidden : HeaderDialogState

    data object Add : HeaderDialogState

    data class Edit(
        val header: ServerRequestHeader,
    ) : HeaderDialogState
}

@Composable
internal fun HeaderEditorDialog(
    initial: ServerRequestHeader?,
    headers: List<ServerRequestHeader>,
    onDismiss: () -> Unit,
    onSave: (ServerRequestHeader) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var value by remember(initial) { mutableStateOf(initial?.value.orEmpty()) }
    val cleaned = remember(name, value) { ServerRequestHeader(name, value).clean() }
    val duplicate =
        remember(cleaned.name, headers, initial) {
            headers.any { it.name == cleaned.name && it.id != initial?.id }
        }
    val nameError =
        when {
            cleaned.name.isEmpty() -> stringResource(R.string.server_connection_header_name_required)
            duplicate -> stringResource(R.string.server_connection_header_duplicate)
            else -> null
        }
    val valueError =
        if (cleaned.value.isEmpty()) {
            stringResource(R.string.server_connection_header_value_required)
        } else {
            null
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    stringResource(
                        if (initial == null) {
                            R.string.server_connection_add_header
                        } else {
                            R.string.server_connection_edit_header
                        },
                    ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(R.string.server_connection_header_name)) },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(text = it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(text = stringResource(R.string.server_connection_header_value)) },
                    singleLine = true,
                    isError = valueError != null,
                    supportingText = valueError?.let { { Text(text = it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(initial?.copy(name = name, value = value) ?: ServerRequestHeader(name, value))
                },
                enabled = nameError == null && valueError == null,
            ) {
                Text(text = stringResource(R.string.server_connection_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}
