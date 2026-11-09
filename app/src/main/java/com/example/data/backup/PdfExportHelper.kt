package com.example.data.backup

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExportHelper(private val database: AppDatabase) {

    suspend fun generatePdf(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val boxes = database.boxDao().getAllBoxesSync()
        val items = database.itemDao().getAllItemsSync()
        val categories = database.categoryDao().getAllCategoriesSync().associateBy { it.id }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(139, 74, 0)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 10f
            isAntiAlias = true
        }

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        var y = 50f

        canvas.drawText("Inventario de Cajas - Catálogo", 40f, y, titlePaint)
        y += 18f
        val totalUnits = items.sumOf { it.quantity }
        canvas.drawText("Generado el: $dateStr · Total: ${boxes.size} cajas, ${items.size} objetos ($totalUnits uds)", 40f, y, subtitlePaint)
        y += 20f

        canvas.drawLine(40f, y, 555f, y, Paint().apply { color = Color.LTGRAY; strokeWidth = 1f })
        y += 20f

        val itemsByBox = items.groupBy { it.boxId }

        for (box in boxes) {
            if (y > 780f) break
            val zoneLabel = if (!box.zone.isNullOrBlank()) " [Zona: ${box.zone}]" else ""
            canvas.drawText("📦 ${box.name}$zoneLabel", 40f, y, headerPaint)
            y += 15f

            val boxItems = itemsByBox[box.id] ?: emptyList()
            if (boxItems.isEmpty()) {
                canvas.drawText("    (Sin objetos)", 45f, y, subtitlePaint)
                y += 14f
            } else {
                for (item in boxItems) {
                    if (y > 800f) break
                    val catName = item.categoryId?.let { categories[it]?.name } ?: "General"
                    val loanInfo = if (item.isLoaned) " [PRESTADO a ${item.loanedTo ?: "alguien"}]" else ""
                    canvas.drawText("  • ${item.name} (x${item.quantity}) - $catName$loanInfo", 45f, y, textPaint)
                    y += 14f
                }
            }
            y += 8f
        }

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}
