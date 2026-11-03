package com.example.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.presentation.DeleteTarget

@Composable
fun DeleteConfirmDialog(
    target: DeleteTarget,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title: String
    val message: String

    when (target) {
        is DeleteTarget.Box -> {
            title = "¿Eliminar caja «${target.box.name}»?"
            message = if (target.subBoxesCount > 0 || target.itemsCount > 0) {
                "Esta caja contiene ${target.subBoxesCount} subcajas y ${target.itemsCount} objetos. Al eliminarla, se borrará todo su contenido de forma permanente."
            } else {
                "¿Seguro que deseas eliminar esta caja vacía?"
            }
        }
        is DeleteTarget.Item -> {
            title = "¿Eliminar objeto?"
            message = "¿Deseas eliminar «${target.item.name}» del inventario?"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Eliminar", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
