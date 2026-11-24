package com.SLM.utils

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.SLM.data.local.entity.DebtStatus
import com.SLM.data.local.entity.DebtType
import com.SLM.data.local.entity.TransactionEntity
import com.SLM.data.local.entity.TransactionType
import com.SLM.data.repository.DebtWithPayments
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

object CurrencyUtils {
    private val decimalFormatLocal = ThreadLocal.withInitial {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    fun formatAmount(amount: Double, symbol: String = "৳"): String {
        val formatter = decimalFormatLocal.get() ?: NumberFormat.getNumberInstance(Locale.US)
        return "$symbol ${formatter.format(amount)}"
    }

    fun formatCompact(amount: Double, symbol: String = "৳"): String {
        val absAmount = kotlin.math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absAmount >= 10000000 -> "$sign$symbol ${String.format(Locale.US, "%.1f", absAmount / 10000000)} Cr"
            absAmount >= 100000 -> "$sign$symbol ${String.format(Locale.US, "%.1f", absAmount / 100000)} Lk"
            absAmount >= 1000 -> "$sign$symbol ${String.format(Locale.US, "%.1f", absAmount / 1000)}k"
            else -> formatAmount(amount, symbol)
        }
    }
}

object DateUtils {
    private val sdfCacheLocal = ThreadLocal.withInitial { mutableMapOf<String, SimpleDateFormat>() }

    private fun getSdf(pattern: String, lang: String): SimpleDateFormat {
        val locale = if (lang == "bn") Locale("bn", "BD") else Locale.US
        val key = "$pattern|$lang"
        val map = sdfCacheLocal.get() ?: mutableMapOf()
        return map.getOrPut(key) { SimpleDateFormat(pattern, locale) }
    }

    fun formatDate(timestamp: Long, lang: String = "en"): String {
        return getSdf("dd MMM yyyy", lang).format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long, lang: String = "en"): String {
        return getSdf("dd MMM yyyy, hh:mm a", lang).format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long, lang: String = "en"): String {
        return getSdf("MMMM yyyy", lang).format(Date(timestamp))
    }

    fun getMonthYearKey(timestamp: Long = System.currentTimeMillis()): String {
        return getSdf("yyyy-MM", "en").format(Date(timestamp))
    }

    fun getDayBounds(daysOffset: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return start to cal.timeInMillis
    }

    fun getWeekBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return start to cal.timeInMillis
    }

    fun getMonthBounds(monthsOffset: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, monthsOffset)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return start to cal.timeInMillis
    }

    fun isToday(timestamp: Long): Boolean {
        val (start, end) = getDayBounds(0)
        return timestamp in start..end
    }

    fun isThisMonth(timestamp: Long): Boolean {
        val (start, end) = getMonthBounds(0)
        return timestamp in start..end
    }
}

object ExportUtils {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_H = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 72 // 523
    private const val FOOTER_Y = PAGE_HEIGHT - 28f
    private const val MAX_Y = PAGE_HEIGHT - 54f

    private fun newPaint(
        colorInt: Int,
        textSize: Float,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT
    ): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            textAlign = align
        }
    }

    private fun drawStaticText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        maxWidth: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Float {
        if (text.isBlank()) return 0f
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth.coerceAtLeast(20))
            .setAlignment(alignment)
            .setLineSpacing(2f, 1.08f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    private fun drawPageHeader(
        canvas: Canvas,
        title: String,
        subtitle: String,
        userName: String,
        generatedAt: String
    ): Float {
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F766E")
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14B8A6")
            alpha = 40
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 108f, headerPaint)
        canvas.drawCircle(PAGE_WIDTH - 40f, 22f, 68f, accentPaint)
        canvas.drawCircle(PAGE_WIDTH - 110f, 96f, 44f, accentPaint)

        val brandPaint = newPaint(Color.parseColor("#CCFBF1"), 10f, bold = true)
        drawStaticText(canvas, "INEX TRACKER • PERSONAL FINANCE REPORT", brandPaint, MARGIN_H, 20f, CONTENT_WIDTH)

        val titlePaint = newPaint(Color.WHITE, 20f, bold = true)
        drawStaticText(canvas, title, titlePaint, MARGIN_H, 36f, CONTENT_WIDTH)

        val subPaint = newPaint(Color.parseColor("#E2E8F0"), 10f, bold = false)
        drawStaticText(canvas, subtitle, subPaint, MARGIN_H, 66f, CONTENT_WIDTH)

        val metaRight = newPaint(Color.parseColor("#F1F5F9"), 9.5f, bold = false)
        val metaText = if (userName.isNotBlank()) "$userName  •  $generatedAt" else generatedAt
        drawStaticText(canvas, metaText, metaRight, MARGIN_H, 84f, CONTENT_WIDTH)

        return 126f
    }

    private fun drawPageFooter(canvas: Canvas, pageNum: Int, lang: String) {
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.8f
        }
        canvas.drawLine(MARGIN_H, FOOTER_Y - 12f, PAGE_WIDTH - MARGIN_H, FOOTER_Y - 12f, dividerPaint)
        val footerPaint = newPaint(Color.parseColor("#64748B"), 9f)
        val leftText = if (lang == "bn") "InEx Tracker অ্যাপ থেকে তৈরি" else "Generated by InEx Tracker"
        drawStaticText(canvas, leftText, footerPaint, MARGIN_H, FOOTER_Y - 4f, 300)
        val pageLabel = if (lang == "bn") "পৃষ্ঠা $pageNum" else "Page $pageNum"
        drawStaticText(canvas, pageLabel, footerPaint, MARGIN_H, FOOTER_Y - 4f, CONTENT_WIDTH, Layout.Alignment.ALIGN_OPPOSITE)
    }

    private fun drawSummaryBox(
        canvas: Canvas,
        rect: RectF,
        bgColor: Int,
        borderColor: Int,
        label: String,
        value: String,
        valueColor: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 10f, 10f, bgPaint)
        canvas.drawRoundRect(rect, 10f, 10f, strokePaint)

        val labelPaint = newPaint(Color.parseColor("#475569"), 9.5f, bold = false)
        val valPaint = newPaint(valueColor, 13.5f, bold = true)
        val w = (rect.width() - 20f).toInt().coerceAtLeast(40)
        drawStaticText(canvas, label, labelPaint, rect.left + 10f, rect.top + 10f, w)
        drawStaticText(canvas, value, valPaint, rect.left + 10f, rect.top + 27f, w)
    }

    fun generateTransactionsPdfFile(
        context: Context,
        transactions: List<TransactionEntity>,
        currencySymbol: String = "৳",
        lang: String = "bn",
        userName: String = ""
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val generatedAt = DateUtils.formatDateTime(System.currentTimeMillis(), lang)
            val title = if (lang == "bn") "লেনদেন বিবরণী (Transactions)" else "Transactions Statement"
            val subtitle = if (lang == "bn") "মোট ${transactions.size}টি লেনদেনের পূর্ণাঙ্গ তালিকা" else "Complete list of ${transactions.size} transactions"

            var pageNum = 1
            var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
            var canvas = page.canvas
            var y = drawPageHeader(canvas, title, subtitle, userName, generatedAt)

            val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val net = totalIncome - totalExpense

            val colW = (CONTENT_WIDTH - 16f) / 3f
            drawSummaryBox(
                canvas,
                RectF(MARGIN_H, y, MARGIN_H + colW, y + 54f),
                Color.parseColor("#ECFDF5"),
                Color.parseColor("#A7F3D0"),
                if (lang == "bn") "মোট আয়" else "Total Income",
                CurrencyUtils.formatAmount(totalIncome, currencySymbol),
                Color.parseColor("#059669")
            )
            drawSummaryBox(
                canvas,
                RectF(MARGIN_H + colW + 8f, y, MARGIN_H + 2 * colW + 8f, y + 54f),
                Color.parseColor("#FEF2F2"),
                Color.parseColor("#FECACA"),
                if (lang == "bn") "মোট ব্যয়" else "Total Expense",
                CurrencyUtils.formatAmount(totalExpense, currencySymbol),
                Color.parseColor("#DC2626")
            )
            drawSummaryBox(
                canvas,
                RectF(MARGIN_H + 2 * colW + 16f, y, MARGIN_H + CONTENT_WIDTH, y + 54f),
                Color.parseColor("#F0FDFA"),
                Color.parseColor("#99F6E4"),
                if (lang == "bn") "নেট উদ্বৃত্ত" else "Net Balance",
                CurrencyUtils.formatAmount(net, currencySymbol),
                Color.parseColor("#0F766E")
            )
            y += 70f

            fun drawTableHeader(c: Canvas, topY: Float): Float {
                val barPaint = Paint().apply { color = Color.parseColor("#E2E8F0") }
                c.drawRoundRect(RectF(MARGIN_H, topY, MARGIN_H + CONTENT_WIDTH, topY + 24f), 6f, 6f, barPaint)
                val hdrPaint = newPaint(Color.parseColor("#0F172A"), 9.5f, bold = true)
                drawStaticText(c, if (lang == "bn") "তারিখ" else "Date", hdrPaint, MARGIN_H + 8f, topY + 6f, 82)
                drawStaticText(c, if (lang == "bn") "ক্যাটাগরি ও বিবরণ" else "Category & Note", hdrPaint, MARGIN_H + 94f, topY + 6f, 220)
                drawStaticText(c, if (lang == "bn") "মাধ্যম" else "Method", hdrPaint, MARGIN_H + 320f, topY + 6f, 75)
                drawStaticText(c, if (lang == "bn") "পরিমাণ" else "Amount", hdrPaint, MARGIN_H + 400f, topY + 6f, 115, Layout.Alignment.ALIGN_OPPOSITE)
                return topY + 30f
            }

            y = drawTableHeader(canvas, y)

            val datePaint = newPaint(Color.parseColor("#475569"), 9f)
            val catPaint = newPaint(Color.parseColor("#0F172A"), 9.5f, bold = true)
            val notePaint = newPaint(Color.parseColor("#64748B"), 8.5f)
            val methodPaint = newPaint(Color.parseColor("#0F766E"), 8.5f, bold = true)
            val incomePaint = newPaint(Color.parseColor("#059669"), 9.5f, bold = true)
            val expensePaint = newPaint(Color.parseColor("#DC2626"), 9.5f, bold = true)
            val rowDivider = Paint().apply { color = Color.parseColor("#F1F5F9"); strokeWidth = 1f }
            val altBg = Paint().apply { color = Color.parseColor("#F8FAFC") }

            transactions.forEachIndexed { idx, tx ->
                val noteH = if (tx.note.isNotBlank()) 14f else 0f
                val rowHeight = 24f + noteH
                if (y + rowHeight > MAX_Y) {
                    drawPageFooter(canvas, pageNum, lang)
                    pdfDocument.finishPage(page)
                    pageNum++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                    canvas = page.canvas
                    y = drawPageHeader(canvas, title, subtitle, userName, generatedAt)
                    y = drawTableHeader(canvas, y)
                }

                if (idx % 2 == 1) {
                    canvas.drawRect(MARGIN_H, y - 2f, MARGIN_H + CONTENT_WIDTH, y + rowHeight - 2f, altBg)
                }

                drawStaticText(canvas, DateUtils.formatDate(tx.date, lang), datePaint, MARGIN_H + 8f, y + 3f, 82)
                drawStaticText(canvas, tx.category, catPaint, MARGIN_H + 94f, y + 2f, 220)
                if (tx.note.isNotBlank()) {
                    drawStaticText(canvas, tx.note.take(48), notePaint, MARGIN_H + 94f, y + 15f, 220)
                }
                drawStaticText(canvas, tx.paymentMethod.name, methodPaint, MARGIN_H + 320f, y + 3f, 75)

                val isIncome = tx.type == TransactionType.INCOME
                val amtStr = (if (isIncome) "+ " else "- ") + CurrencyUtils.formatAmount(tx.amount, currencySymbol)
                drawStaticText(
                    canvas,
                    amtStr,
                    if (isIncome) incomePaint else expensePaint,
                    MARGIN_H + 400f,
                    y + 3f,
                    115,
                    Layout.Alignment.ALIGN_OPPOSITE
                )
                y += rowHeight
                canvas.drawLine(MARGIN_H, y - 2f, MARGIN_H + CONTENT_WIDTH, y - 2f, rowDivider)
            }

            drawPageFooter(canvas, pageNum, lang)
            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "InExTracker_Transactions_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateDebtsPdfFile(
        context: Context,
        debts: List<DebtWithPayments>,
        currencySymbol: String = "৳",
        lang: String = "bn",
        userName: String = ""
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val generatedAt = DateUtils.formatDateTime(System.currentTimeMillis(), lang)
            val title = if (lang == "bn") "দেনা-পাওনা খাতা রিপোর্ট" else "Debt & Receivable Ledger"
            val subtitle = if (lang == "bn") "মোট ${debts.size}টি খাতা এন্ট্রির বিবরণ" else "Summary of ${debts.size} debt entries"

            var pageNum = 1
            var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
            var canvas = page.canvas
            var y = drawPageHeader(canvas, title, subtitle, userName, generatedAt)

            val totalReceivable = debts.filter { it.debt.debtType == DebtType.RECEIVABLE }.sumOf { it.remainingAmount }
            val totalPayable = debts.filter { it.debt.debtType == DebtType.PAYABLE }.sumOf { it.remainingAmount }

            val halfW = (CONTENT_WIDTH - 10f) / 2f
            drawSummaryBox(
                canvas,
                RectF(MARGIN_H, y, MARGIN_H + halfW, y + 54f),
                Color.parseColor("#EEF2FF"),
                Color.parseColor("#C7D2FE"),
                if (lang == "bn") "মোট পাওনা (আমি পাবো)" else "Total Receivable",
                CurrencyUtils.formatAmount(totalReceivable, currencySymbol),
                Color.parseColor("#4F46E5")
            )
            drawSummaryBox(
                canvas,
                RectF(MARGIN_H + halfW + 10f, y, MARGIN_H + CONTENT_WIDTH, y + 54f),
                Color.parseColor("#FFF7ED"),
                Color.parseColor("#FED7AA"),
                if (lang == "bn") "মোট দেনা (দিতে হবে)" else "Total Payable",
                CurrencyUtils.formatAmount(totalPayable, currencySymbol),
                Color.parseColor("#EA580C")
            )
            y += 70f

            fun drawDebtHeader(c: Canvas, topY: Float): Float {
                val barPaint = Paint().apply { color = Color.parseColor("#E2E8F0") }
                c.drawRoundRect(RectF(MARGIN_H, topY, MARGIN_H + CONTENT_WIDTH, topY + 24f), 6f, 6f, barPaint)
                val hdrPaint = newPaint(Color.parseColor("#0F172A"), 9f, bold = true)
                drawStaticText(c, if (lang == "bn") "নাম ও ধরন" else "Person & Type", hdrPaint, MARGIN_H + 8f, topY + 6f, 165)
                drawStaticText(c, if (lang == "bn") "মোট" else "Total", hdrPaint, MARGIN_H + 178f, topY + 6f, 82, Layout.Alignment.ALIGN_OPPOSITE)
                drawStaticText(c, if (lang == "bn") "পরিশোধ" else "Paid", hdrPaint, MARGIN_H + 266f, topY + 6f, 82, Layout.Alignment.ALIGN_OPPOSITE)
                drawStaticText(c, if (lang == "bn") "বাকি" else "Remaining", hdrPaint, MARGIN_H + 354f, topY + 6f, 92, Layout.Alignment.ALIGN_OPPOSITE)
                drawStaticText(c, if (lang == "bn") "অবস্থা" else "Status", hdrPaint, MARGIN_H + 452f, topY + 6f, 64, Layout.Alignment.ALIGN_OPPOSITE)
                return topY + 30f
            }

            y = drawDebtHeader(canvas, y)

            val namePaint = newPaint(Color.parseColor("#0F172A"), 9.5f, bold = true)
            val subPaint = newPaint(Color.parseColor("#64748B"), 8.5f)
            val numPaint = newPaint(Color.parseColor("#334155"), 9f)
            val paidPaint = newPaint(Color.parseColor("#059669"), 9f, bold = true)
            val recPaint = newPaint(Color.parseColor("#4F46E5"), 9.5f, bold = true)
            val payPaint = newPaint(Color.parseColor("#EA580C"), 9.5f, bold = true)
            val rowDivider = Paint().apply { color = Color.parseColor("#F1F5F9"); strokeWidth = 1f }
            val altBg = Paint().apply { color = Color.parseColor("#F8FAFC") }

            debts.forEachIndexed { idx, item ->
                val d = item.debt
                val rowHeight = if (d.phoneNumber.isNotBlank() || d.dueDate != null) 38f else 28f
                if (y + rowHeight > MAX_Y) {
                    drawPageFooter(canvas, pageNum, lang)
                    pdfDocument.finishPage(page)
                    pageNum++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                    canvas = page.canvas
                    y = drawPageHeader(canvas, title, subtitle, userName, generatedAt)
                    y = drawDebtHeader(canvas, y)
                }

                if (idx % 2 == 1) {
                    canvas.drawRect(MARGIN_H, y - 2f, MARGIN_H + CONTENT_WIDTH, y + rowHeight - 2f, altBg)
                }

                val isRec = d.debtType == DebtType.RECEIVABLE
                val typeLabel = if (lang == "bn") {
                    if (isRec) "পাওনা" else "দেনা"
                } else {
                    if (isRec) "Receivable" else "Payable"
                }
                drawStaticText(canvas, "${d.personName} ($typeLabel)", namePaint, MARGIN_H + 8f, y + 2f, 165)

                val metaLine = buildString {
                    if (d.phoneNumber.isNotBlank()) append(d.phoneNumber)
                    if (d.dueDate != null) {
                        if (isNotEmpty()) append(" • ")
                        append(DateUtils.formatDate(d.dueDate, lang))
                    }
                }
                if (metaLine.isNotBlank()) {
                    drawStaticText(canvas, metaLine, subPaint, MARGIN_H + 8f, y + 16f, 165)
                }

                drawStaticText(canvas, CurrencyUtils.formatAmount(d.totalAmount, currencySymbol), numPaint, MARGIN_H + 178f, y + 4f, 82, Layout.Alignment.ALIGN_OPPOSITE)
                drawStaticText(canvas, CurrencyUtils.formatAmount(item.totalPaid, currencySymbol), paidPaint, MARGIN_H + 266f, y + 4f, 82, Layout.Alignment.ALIGN_OPPOSITE)
                drawStaticText(canvas, CurrencyUtils.formatAmount(item.remainingAmount, currencySymbol), if (isRec) recPaint else payPaint, MARGIN_H + 354f, y + 4f, 92, Layout.Alignment.ALIGN_OPPOSITE)

                val statusText = when (item.status) {
                    DebtStatus.PAID -> if (lang == "bn") "পরিশোধ" else "Paid"
                    DebtStatus.OVERDUE -> if (lang == "bn") "মেয়াদোত্তীর্ণ" else "Overdue"
                    DebtStatus.PARTIALLY_PAID -> if (lang == "bn") "আংশিক" else "Partial"
                    DebtStatus.PENDING -> if (lang == "bn") "বাকি" else "Pending"
                }
                drawStaticText(canvas, statusText, subPaint, MARGIN_H + 452f, y + 4f, 64, Layout.Alignment.ALIGN_OPPOSITE)

                y += rowHeight
                canvas.drawLine(MARGIN_H, y - 2f, MARGIN_H + CONTENT_WIDTH, y - 2f, rowDivider)
            }

            drawPageFooter(canvas, pageNum, lang)
            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "InExTracker_DebtBook_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateFinancialReportPdfFile(
        context: Context,
        periodLabel: String,
        totalIncome: Double,
        totalExpense: Double,
        balance: Double,
        totalReceivable: Double,
        totalPayable: Double,
        transactions: List<TransactionEntity>,
        currencySymbol: String = "৳",
        lang: String = "bn",
        userName: String = ""
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val generatedAt = DateUtils.formatDateTime(System.currentTimeMillis(), lang)
            val title = if (lang == "bn") "আর্থিক রিপোর্ট ও বিশ্লেষণ" else "Financial Report & Analytics"
            val subtitle = if (lang == "bn") "সময়কাল: $periodLabel • ${transactions.size}টি লেনদেন" else "Period: $periodLabel • ${transactions.size} transactions"

            var pageNum = 1
            var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
            var canvas = page.canvas
            var y = drawPageHeader(canvas, title, subtitle, userName, generatedAt)

            // Summary Grid (2 rows)
            val col3W = (CONTENT_WIDTH - 16f) / 3f
            drawSummaryBox(
                canvas,
                RectF(MARGIN_H, y, MARGIN_H + col3W, y + 54f),
                Color.parseColor("#ECFDF5"),
                Color.parseColor("#A7F3D0"),
                if (lang == "bn") "মোট আয়" else "Total Income",
                CurrencyUtils.formatAmount(totalIncome, currencySymbol),
                Color.parseColor("#059669")
            )
            drawSummaryBox(
                canvas,
                RectF(MARGIN_H + col3W + 8f, y, MARGIN_H + 2 * col3W + 8f, y + 54f),
                Color.parseColor("#FEF2F2"),
                Color.parseColor("#FECACA"),
                if (lang == "bn") "মোট ব্যয়" else "Total Expense",
                CurrencyUtils.formatAmount(totalExpense, currencySymbol),
                Color.parseColor("#DC2626")
            )
            drawSummaryBox(
                canvas,
                RectF(MARGIN_H + 2 * col3W + 16f, y, MARGIN_H + CONTENT_WIDTH, y + 54f),
                Color.parseColor("#F0FDFA"),
                Color.parseColor("#99F6E4"),
                if (lang == "bn") "নেট সঞ্চয়" else "Net Savings",
                CurrencyUtils.formatAmount(balance, currencySymbol),
                if (balance >= 0) Color.parseColor("#0F766E") else Color.parseColor("#DC2626")
            )
            y += 62f

            val halfW = (CONTENT_WIDTH - 10f) / 2f
            drawSummaryBox(
                canvas,
                RectF(MARGIN_H, y, MARGIN_H + halfW, y + 50f),
                Color.parseColor("#EEF2FF"),
                Color.parseColor("#C7D2FE"),
                if (lang == "bn") "মোট পাওনা (Receivable)" else "Total Receivable",
                CurrencyUtils.formatAmount(totalReceivable, currencySymbol),
                Color.parseColor("#4F46E5")
            )
            drawSummaryBox(
                canvas,
                RectF(MARGIN_H + halfW + 10f, y, MARGIN_H + CONTENT_WIDTH, y + 50f),
                Color.parseColor("#FFF7ED"),
                Color.parseColor("#FED7AA"),
                if (lang == "bn") "মোট দেনা (Payable)" else "Total Payable",
                CurrencyUtils.formatAmount(totalPayable, currencySymbol),
                Color.parseColor("#EA580C")
            )
            y += 68f

            // Category Breakdown Section
            val expenseByCategory = transactions.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toList()
                .sortedByDescending { it.second }

            val sectionPaint = newPaint(Color.parseColor("#0F172A"), 12f, bold = true)
            if (expenseByCategory.isNotEmpty()) {
                drawStaticText(
                    canvas,
                    if (lang == "bn") "ক্যাটাগরি ভিত্তিক ব্যয়" else "Expense Breakdown by Category",
                    sectionPaint,
                    MARGIN_H,
                    y,
                    CONTENT_WIDTH
                )
                y += 20f

                val catNamePaint = newPaint(Color.parseColor("#1E293B"), 9.5f, bold = true)
                val catAmtPaint = newPaint(Color.parseColor("#DC2626"), 9.5f, bold = true)
                val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E2E8F0") }
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EF4444") }

                expenseByCategory.take(10).forEach { (cat, amt) ->
                    if (y + 26f > MAX_Y) {
                        drawPageFooter(canvas, pageNum, lang)
                        pdfDocument.finishPage(page)
                        pageNum++
                        page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                        canvas = page.canvas
                        y = drawPageHeader(canvas, title, subtitle, userName, generatedAt)
                    }
                    val pct = if (totalExpense > 0) (amt / totalExpense * 100).toInt() else 0
                    val ratio = if (totalExpense > 0) (amt / totalExpense).toFloat().coerceIn(0.02f, 1f) else 0f
                    drawStaticText(canvas, "$cat ($pct%)", catNamePaint, MARGIN_H, y, 320)
                    drawStaticText(canvas, CurrencyUtils.formatAmount(amt, currencySymbol), catAmtPaint, MARGIN_H + 330f, y, 190, Layout.Alignment.ALIGN_OPPOSITE)
                    y += 14f
                    canvas.drawRoundRect(RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + 5f), 2.5f, 2.5f, trackPaint)
                    canvas.drawRoundRect(RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH * ratio, y + 5f), 2.5f, 2.5f, fillPaint)
                    y += 13f
                }
                y += 10f
            }

            // Transaction List Section
            if (transactions.isNotEmpty()) {
                if (y + 60f > MAX_Y) {
                    drawPageFooter(canvas, pageNum, lang)
                    pdfDocument.finishPage(page)
                    pageNum++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                    canvas = page.canvas
                    y = drawPageHeader(canvas, title, subtitle, userName, generatedAt)
                }

                drawStaticText(
                    canvas,
                    if (lang == "bn") "লেনদেনের তালিকা" else "Transaction Details",
                    sectionPaint,
                    MARGIN_H,
                    y,
                    CONTENT_WIDTH
                )
                y += 20f

                val datePaint = newPaint(Color.parseColor("#475569"), 9f)
                val catPaint = newPaint(Color.parseColor("#0F172A"), 9.5f, bold = true)
                val pmPaint = newPaint(Color.parseColor("#0F766E"), 8.5f)
                val incPaint = newPaint(Color.parseColor("#059669"), 9.5f, bold = true)
                val expPaint = newPaint(Color.parseColor("#DC2626"), 9.5f, bold = true)
                val divider = Paint().apply { color = Color.parseColor("#F1F5F9"); strokeWidth = 1f }

                transactions.forEach { tx ->
                    if (y + 24f > MAX_Y) {
                        drawPageFooter(canvas, pageNum, lang)
                        pdfDocument.finishPage(page)
                        pageNum++
                        page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                        canvas = page.canvas
                        y = drawPageHeader(canvas, title, subtitle, userName, generatedAt)
                    }
                    drawStaticText(canvas, DateUtils.formatDate(tx.date, lang), datePaint, MARGIN_H, y + 2f, 82)
                    val titleStr = if (tx.note.isNotBlank()) "${tx.category} — ${tx.note.take(28)}" else tx.category
                    drawStaticText(canvas, titleStr, catPaint, MARGIN_H + 88f, y + 2f, 240)
                    drawStaticText(canvas, tx.paymentMethod.name, pmPaint, MARGIN_H + 334f, y + 2f, 70)
                    val isInc = tx.type == TransactionType.INCOME
                    val amtText = (if (isInc) "+ " else "- ") + CurrencyUtils.formatAmount(tx.amount, currencySymbol)
                    drawStaticText(canvas, amtText, if (isInc) incPaint else expPaint, MARGIN_H + 408f, y + 2f, 114, Layout.Alignment.ALIGN_OPPOSITE)
                    y += 22f
                    canvas.drawLine(MARGIN_H, y - 2f, MARGIN_H + CONTENT_WIDTH, y - 2f, divider)
                }
            }

            drawPageFooter(canvas, pageNum, lang)
            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "InExTracker_Report_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun savePdfToDownloads(context: Context, sourceFile: File): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/InExTracker")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                val hasStoragePerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                val baseDownloads = if (hasStoragePerm) {
                    @Suppress("DEPRECATION")
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                } else {
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                }
                val targetFolder = File(baseDownloads, "InExTracker")
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs()
                }
                val dest = File(targetFolder, sourceFile.name)
                sourceFile.copyTo(dest, overwrite = true)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(dest.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun writeFileToUri(context: Context, sourceFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun sharePdfFile(context: Context, pdfFile: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openPdfFile(context: Context, pdfFile: File, fallbackTitle: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(viewIntent, fallbackTitle))
        } catch (e: Exception) {
            sharePdfFile(context, pdfFile, fallbackTitle)
        }
    }
}
