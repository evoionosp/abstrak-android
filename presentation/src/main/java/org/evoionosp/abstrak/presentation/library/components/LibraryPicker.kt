package org.evoionosp.abstrak.presentation.library.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.header.HeaderPill
import org.evoionosp.abstrak.presentation.designsystem.component.header.HeaderPillDefaults
import org.evoionosp.abstrak.presentation.designsystem.component.header.HeaderPillTitle
import org.evoionosp.abstrak.presentation.designsystem.component.menu.AbstrakDropdownMenu
import org.evoionosp.abstrak.presentation.ui.model.LibraryUiModel

@Composable
internal fun LibraryPicker(
    libraries: List<LibraryUiModel>,
    selectedLibraryId: String?,
    selectedLibraryName: String?,
    onLibrarySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (libraries.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    var buttonWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Box(modifier = modifier) {
        HeaderPill(
            modifier =
                Modifier.onSizeChanged { size ->
                    buttonWidth = with(density) { size.width.toDp() }
                },
        ) {
            TextButton(
                onClick = { expanded = true },
                contentPadding = HeaderPillDefaults.ContentPadding,
            ) {
                HeaderPillTitle(
                    text = selectedLibraryName.orEmpty(),
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = stringResource(R.string.library_dropdown_label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AbstrakDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(buttonWidth),
        ) {
            libraries.forEach { library ->
                val isSelected = library.id == selectedLibraryId
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = library.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight =
                                    if (isSelected) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        if (library.id != selectedLibraryId) {
                            onLibrarySelected(library.id)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors =
                        MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
