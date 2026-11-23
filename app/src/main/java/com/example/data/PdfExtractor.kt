package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

private const val MAX_PDF_BYTES = 8 * 1024 * 1024 // 8 MB

data class ExtractedPdfResult(
    val fileName: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val extractedText: String,
    val pageBitmaps: List<Bitmap>,
)

object PdfExtractor {

    fun extract(context: Context, uri: Uri): Result<ExtractedPdfResult> = runCatching {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri).orEmpty()
        var displayName = "health_report.pdf"
        var reportedSize = -1L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) {
                    displayName = cursor.getString(nameIdx) ?: displayName
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    reportedSize = cursor.getLong(sizeIdx)
                }
            }
        }

        if (mimeType.isNotBlank() && mimeType != "application/pdf" && !displayName.endsWith(".pdf", ignoreCase = true)) {
            throw IllegalArgumentException("Only PDF files (.pdf) are supported.")
        }

        if (reportedSize > MAX_PDF_BYTES) {
            throw IllegalArgumentException("PDF exceeds the 8 MB maximum file size.")
        }

        val bytes = resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = input.read(chunk)
                if (read == -1) break
                total += read
                if (total > MAX_PDF_BYTES) {
                    throw IllegalArgumentException("PDF exceeds the 8 MB maximum file size.")
                }
                buffer.write(chunk, 0, read)
            }
            buffer.toByteArray()
        } ?: throw IllegalStateException("Unable to read the selected PDF file.")

        if (bytes.size < 5 || String(bytes, 0, 5, StandardCharsets.US_ASCII) != "%PDF-") {
            throw IllegalArgumentException("The selected file does not have a valid PDF header.")
        }

        val bitmaps = mutableListOf<Bitmap>()
        var pageCount = 1

        resolver.openFileDescriptor(uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                pageCount = renderer.pageCount.coerceAtLeast(1)
                val pagesToRender = minOf(pageCount, 3)
                for (index in 0 until pagesToRender) {
                    renderer.openPage(index).use { page ->
                        val targetWidth = 1024
                        val scale = targetWidth.toFloat() / page.width.coerceAtLeast(1)
                        val targetHeight = (page.height * scale).toInt().coerceIn(256, 1600)
                        val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        canvas.drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bmp)
                    }
                }
            }
        }

        val rawText = extractAsciiLiterals(bytes)
        ExtractedPdfResult(
            fileName = displayName.take(160),
            fileSizeBytes = bytes.size.toLong(),
            pageCount = pageCount,
            extractedText = rawText.take(45_000),
            pageBitmaps = bitmaps,
        )
    }

    private fun extractAsciiLiterals(pdfBytes: ByteArray): String {
        val raw = String(pdfBytes, StandardCharsets.ISO_8859_1)
        val sb = StringBuilder()
        val tjPattern = Regex("\\(([^()\\\\]{2,120})\\)\\s*Tj")
        for (match in tjPattern.findAll(raw)) {
            val snippet = match.groupValues[1]
                .replace("\\(", "(")
                .replace("\\)", ")")
                .trim()
            if (snippet.any { it.isLetterOrDigit() }) {
                sb.append(snippet).append(' ')
            }
            if (sb.length >= 12_000) break
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}
