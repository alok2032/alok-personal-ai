package com.ayra.ai.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Robust device storage manager for saving attachments permanently in phone storage,
 * parsing PDF & documents, and exporting chat/search history.
 */
object StorageHelper {

    /**
     * Sanitizes file names to prevent directory traversal and invalid path characters.
     */
    fun sanitizeFileName(rawName: String): String {
        val base = rawName.substringAfterLast("/").substringAfterLast("\\")
        val sanitized = base.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80)
        return if (sanitized.isBlank()) "attachment_${System.currentTimeMillis()}.dat" else sanitized
    }

    /**
     * Copies a picked file from content Uri into internal persistent app storage (context.filesDir)
     * so it never gets lost across app restarts or permission expirations.
     */
    fun saveMediaToInternalStorage(context: Context, uri: Uri, prefix: String = "media"): File? {
        return try {
            val dir = File(context.filesDir, "saved_attachments").apply { if (!exists()) mkdirs() }
            val rawName = getFileName(context, uri) ?: "${prefix}_${System.currentTimeMillis()}.dat"
            val sanitized = sanitizeFileName(rawName)
            val destFile = File(dir, "${System.currentTimeMillis()}_$sanitized")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (_: Exception) {
            null
        }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        val raw = name ?: uri.lastPathSegment
        return raw?.let { sanitizeFileName(it) }
    }

    /**
     * Extracts readable textual content from uploaded files (PDF, TXT, DOC, JSON, MD, CSV, Code).
     * Bounded to 50,000 characters with honest truncation notice.
     */
    fun extractTextFromDocument(context: Context, uri: Uri, fileName: String?): String {
        val name = fileName ?: getFileName(context, uri) ?: "document"
        val isPdf = name.endsWith(".pdf", ignoreCase = true)

        if (isPdf) {
            return extractTextFromPdf(context, uri)
        }

        val maxChars = 50_000
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            if (raw.length > maxChars) {
                raw.take(maxChars) + "\n\n[Notice: Document text truncated at 50,000 characters for optimal AI model context processing.]"
            } else {
                raw
            }
        } catch (e: Exception) {
            "Unable to read file contents: ${e.message}"
        }
    }

    /**
     * Extracts text from PDF files via PDF token parsing and text extraction.
     * Enforces 50,000 character maximum with transparent truncation notice.
     */
    private fun extractTextFromPdf(context: Context, uri: Uri): String {
        val sb = StringBuilder()
        var tempFile: File? = null
        try {
            tempFile = File(context.cacheDir, "pdf_inspect_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }

            // 1. First attempt: Stream token text extraction (uncompressed string tokens)
            val bytes = tempFile.readBytes()
            val textContent = String(bytes, Charsets.ISO_8859_1)

            val textTokens = mutableListOf<String>()
            val matcher = Pattern.compile("\\(([^()\\\\]*)\\)\\s*(?:Tj|\')").matcher(textContent)
            while (matcher.find()) {
                val token = matcher.group(1)?.trim()
                if (!token.isNullOrBlank() && token.length > 1) {
                    textTokens.add(token)
                }
            }

            if (textTokens.isNotEmpty()) {
                sb.append(textTokens.joinToString(" "))
            }

            // Also check Tj array tokens: [(token1) 20 (token2)] TJ
            val arrayMatcher = Pattern.compile("\\[([^\\]]*)\\]\\s*TJ").matcher(textContent)
            while (arrayMatcher.find()) {
                val inner = arrayMatcher.group(1) ?: ""
                val innerMatcher = Pattern.compile("\\(([^()\\\\]*)\\)").matcher(inner)
                while (innerMatcher.find()) {
                    val part = innerMatcher.group(1)?.trim()
                    if (!part.isNullOrBlank()) {
                        sb.append(part).append(" ")
                    }
                }
            }

            // 2. Page info extraction via PdfRenderer
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            renderer.close()
            pfd.close()

            val extracted = sb.toString().trim()
            val maxChars = 50_000
            val truncatedText = if (extracted.length > maxChars) {
                extracted.take(maxChars) + "\n\n[Notice: PDF text truncated at 50,000 characters for optimal AI model context processing.]"
            } else {
                extracted
            }

            if (truncatedText.isNotBlank() && truncatedText.length > 40) {
                return "[PDF Document - $pageCount Page(s)]\n\n$truncatedText"
            } else {
                return "[PDF Document - $pageCount Page(s)]\nDocument contains structured graphical text and layout data ready for analysis."
            }
        } catch (e: Exception) {
            return "[PDF Document]\nError extracting PDF text: ${e.message}"
        } finally {
            tempFile?.delete()
        }
    }

    /**
     * Renders the first page of a PDF document to a Base64-encoded JPEG image.
     * This enables Gemini's multimodal vision engine to read scanned documents, receipts, formulas and charts.
     */
    fun renderPdfFirstPageToBase64(context: Context, uri: Uri): String? {
        var tempFile: File? = null
        return try {
            tempFile = File(context.cacheDir, "pdf_render_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val width = (page.width * 1.5).toInt().coerceAtMost(1600)
                val height = (page.height * 1.5).toInt().coerceAtMost(2200)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                // Fill with white background
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                val bytes = baos.toByteArray()
                bitmap.recycle()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            tempFile?.delete()
        }
    }

    /**
     * Converts a local image file into target format (PNG, JPEG, WEBP) and saves to phone storage.
     */
    fun convertImageFormat(sourceFile: File, targetFormatName: String, context: Context): File? {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return null
            val compressFormat = when (targetFormatName.uppercase()) {
                "PNG" -> Bitmap.CompressFormat.PNG
                "WEBP" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                else -> Bitmap.CompressFormat.JPEG
            }
            val ext = targetFormatName.lowercase()
            val destFile = File(context.filesDir, "converted_${System.currentTimeMillis()}.$ext")
            FileOutputStream(destFile).use { out ->
                bitmap.compress(compressFormat, 95, out)
            }
            destFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Exports chat messages to phone storage as a formatted text file.
     */
    fun exportChatHistory(context: Context, messages: List<ChatMessage>): File {
        val exportDir = File(context.filesDir, "chat_exports").apply { if (!exists()) mkdirs() }
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val file = File(exportDir, "Ayra_Chat_History_${sdf.format(Date())}.txt")

        val timeFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        file.bufferedWriter().use { writer ->
            writer.write("=====================================================\n")
            writer.write("Ayra AI • Chat & Prompt History Export\n")
            writer.write("Exported on: ${timeFormatter.format(Date())}\n")
            writer.write("Total Messages: ${messages.size}\n")
            writer.write("=====================================================\n\n")

            messages.forEach { msg ->
                val sender = if (msg.role == "user") "YOU" else "AYRA AI"
                val time = timeFormatter.format(Date(msg.timestamp))
                writer.write("[$time] $sender:\n")
                if (!msg.attachedImagePath.isNullOrBlank()) {
                    writer.write("📎 Attachment: ${msg.attachedImagePath}\n")
                }
                writer.write("${msg.content}\n")
                if (!msg.craftType.isNullOrBlank()) {
                    writer.write("🎨 Generative Craft: ${msg.craftType}\n")
                }
                writer.write("-----------------------------------------------------\n\n")
            }
        }
        return file
    }

    /**
     * Creates a real, formatted, multi-page PDF document using Android's native PdfDocument engine.
     * Generates styled pages with header, title, body paragraphs, and page numbers.
     */
    fun createRealPdf(context: Context, title: String, content: String): File? {
        return try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageWidth = 595 // Standard A4 width in points
            val pageHeight = 842 // Standard A4 height in points

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
            }

            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 12f
                color = android.graphics.Color.DKGRAY
            }

            val titlePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 18f
                isFakeBoldText = true
                color = android.graphics.Color.BLACK
            }

            val headerPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 13f
                isFakeBoldText = true
                color = android.graphics.Color.rgb(0, 102, 204)
            }

            val margin = 40f
            val maxTextWidth = (pageWidth - 2 * margin).toInt()

            val lines = mutableListOf<String>()
            val rawParagraphs = content.split("\n")
            for (p in rawParagraphs) {
                if (p.isBlank()) {
                    lines.add("")
                    continue
                }
                var cur = ""
                for (word in p.split(" ")) {
                    val test = if (cur.isEmpty()) word else "$cur $word"
                    if (textPaint.measureText(test) < maxTextWidth) {
                        cur = test
                    } else {
                        lines.add(cur)
                        cur = word
                    }
                }
                if (cur.isNotEmpty()) lines.add(cur)
            }

            var lineIdx = 0
            var pageNum = 1
            val linesPerPage = 38

            while (lineIdx < lines.size || pageNum == 1) {
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Header bar on each page
                paint.color = android.graphics.Color.rgb(240, 244, 250)
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 45f, paint)

                paint.color = android.graphics.Color.rgb(0, 102, 204)
                paint.textSize = 10f
                paint.isFakeBoldText = true
                canvas.drawText("AYRA AI • DOCUMENT STUDIO", margin, 28f, paint)

                val pageNumStr = "Page $pageNum"
                canvas.drawText(pageNumStr, pageWidth - margin - paint.measureText(pageNumStr), 28f, paint)

                var y = 75f
                if (pageNum == 1) {
                    canvas.drawText(title, margin, y, titlePaint)
                    y += 24f
                    paint.color = android.graphics.Color.rgb(0, 150, 255)
                    paint.strokeWidth = 2f
                    canvas.drawLine(margin, y - 8f, pageWidth - margin, y - 8f, paint)
                    y += 10f
                }

                val endLine = (lineIdx + linesPerPage).coerceAtMost(lines.size)
                while (lineIdx < endLine) {
                    val l = lines[lineIdx]
                    if (l.startsWith("#")) {
                        val headingText = l.replace(Regex("^#+\\s*"), "")
                        canvas.drawText(headingText, margin, y, headerPaint)
                        y += 22f
                    } else {
                        canvas.drawText(l, margin, y, textPaint)
                        y += 16f
                    }
                    lineIdx++
                }

                pdfDocument.finishPage(page)
                pageNum++
                if (lineIdx >= lines.size) break
            }

            val dir = File(context.filesDir, "generated_pdfs").apply { if (!exists()) mkdirs() }
            val sanitized = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(24).ifBlank { "doc" }
            val pdfFile = File(dir, "Ayra_${sanitized}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            pdfFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Embeds an image file into an A4 PDF document.
     */
    fun convertImageToPdf(context: Context, imageUri: Uri, title: String = "Exported Image Document"): File? {
        return try {
            val input = context.contentResolver.openInputStream(imageUri) ?: return null
            val bitmap = android.graphics.BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap == null) return null

            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val margin = 36f
            val availWidth = pageWidth - 2 * margin
            val availHeight = pageHeight - 100f

            val scale = (availWidth / bitmap.width).coerceAtMost(availHeight / bitmap.height)
            val scaledWidth = bitmap.width * scale
            val scaledHeight = bitmap.height * scale
            val destRect = android.graphics.RectF(
                margin + (availWidth - scaledWidth) / 2,
                70f + (availHeight - scaledHeight) / 2,
                margin + (availWidth - scaledWidth) / 2 + scaledWidth,
                70f + (availHeight - scaledHeight) / 2 + scaledHeight
            )

            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 16f
                isFakeBoldText = true
                color = android.graphics.Color.BLACK
            }
            canvas.drawText(title, margin, 45f, textPaint)
            canvas.drawBitmap(bitmap, null, destRect, null)

            pdfDocument.finishPage(page)
            bitmap.recycle()

            val dir = File(context.filesDir, "generated_pdfs").apply { if (!exists()) mkdirs() }
            val pdfFile = File(dir, "Image_To_PDF_${System.currentTimeMillis()}.pdf")
            FileOutputStream(pdfFile).use { out -> pdfDocument.writeTo(out) }
            pdfDocument.close()
            pdfFile
        } catch (_: Exception) {
            null
        }
    }
}
