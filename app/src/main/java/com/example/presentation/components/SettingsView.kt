package com.example.presentation.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * SettingsView adapter delegating to SettingsScreen.
 */
@Composable
fun SettingsView(
    onOpenBackup: () -> Unit,
    onOpenStats: () -> Unit = {},
    totalBoxesCount: Int = 0,
    totalItemsCount: Int = 0,
    totalItemUnitsCount: Int = 0,
    storageUsageMb: String = "1.4 MB",
    boxViewMode: String = "Portadas",
    gridColumns: Int = 2,
    onViewModeChange: (String) -> Unit = {},
    onGridColumnsChange: (Int) -> Unit = {},
    onExportCsvToFile: ((Uri) -> Unit)? = null,
    onExportPdfToFile: ((Uri) -> Unit)? = null,
    onImportJsonFromFile: ((Uri) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SettingsScreen(
        totalBoxesCount = totalBoxesCount,
        totalItemsCount = totalItemsCount,
        totalItemUnitsCount = totalItemUnitsCount,
        storageUsageMb = storageUsageMb,
        boxViewMode = boxViewMode,
        gridColumns = gridColumns,
        onViewModeChange = onViewModeChange,
        onGridColumnsChange = onGridColumnsChange,
        onOpenBackup = onOpenBackup,
        onExportCsvToFile = onExportCsvToFile,
        onExportPdfToFile = onExportPdfToFile,
        onImportJsonFromFile = onImportJsonFromFile,
        modifier = modifier
    )
}
