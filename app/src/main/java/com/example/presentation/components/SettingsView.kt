package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Settings / Configuración view matching Stitch AI prototype
 * (view-settings in prototipo_navegable_interactivo_amoled_clics_reales/code.html)
 */
@Composable
fun SettingsView(
    onOpenBackup: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAmoledActive by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Preferencias, copias de seguridad y tema",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA6978A)
            )
        }

        // AMOLED Theme Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF14110E),
            border = BorderStroke(1.dp, Color(0xFF28211B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF26180F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Tema AMOLED Puro",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Negro absoluto #000000 para ahorro de batería",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8E8175)
                        )
                    }
                }

                Switch(
                    checked = isAmoledActive,
                    onCheckedChange = { isAmoledActive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4D2700),
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color(0xFF8E8175),
                        uncheckedTrackColor = Color(0xFF201811)
                    )
                )
            }
        }

        // Backup Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF14110E),
            border = BorderStroke(1.dp, Color(0xFF28211B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF132219)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Copia de Seguridad",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Exportar e importar JSON y CSV",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8E8175)
                        )
                    }
                }

                Button(
                    onClick = onOpenBackup,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF201811),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, Color(0xFF3E2B1B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Gestionar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Stats Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF14110E),
            border = BorderStroke(1.dp, Color(0xFF28211B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C1B2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Panel de Estadísticas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Desglose por categorías y cajas",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8E8175)
                        )
                    }
                }

                Button(
                    onClick = onOpenStats,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF201811),
                        contentColor = Color(0xFF8B5CF6)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF38294E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ver", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Info & Branding
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(com.example.R.drawable.cajas_app_logo_1793724857669),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Inventario de Cajas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Versión 2.4 · Google Stitch AI Design System",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8E8175)
            )
            Text(
                text = "Diseño cálido táctil & Material You M3",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF5E5349),
                fontSize = 10.sp
            )
        }
    }
}
