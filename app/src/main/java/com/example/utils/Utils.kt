package com.example.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.repository.DebtWithPayments
import java.io.File
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CurrencyUtils {
    fun formatAmount(amount: Double, symbol: String = "৳"): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$symbol ${formatter.format(amount)}"
    }

    fun formatCompact(amount: Double, symbol: String = "৳"): String {
        return when {
            amount >= 10000000 -> "$symbol ${String.format(Locale.US, "%.1f", amount / 10000000)} Cr"
            amount >= 100000 -> "$symbol ${String.format(Locale.US, "%.1f", amount / 100000)} Lk"
            amount >= 1000 -> "$symbol ${String.format(Locale.US, "%.1f", amount / 1000)}k"
            else -> formatAmount(amount, symbol)
        }
    }
}

object DateUtils {
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getMonthYearKey(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun isToday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isThisMonth(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }
}

object ExportUtils {

    fun exportTransactionsToCsv(context: Context, transactions: List<TransactionEntity>, currencySymbol: String = "৳") {
        try {
            val fileName = "HisabKhata_Transactions_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("ID,Type,Category,Amount,Payment Method,Date,Note\n")
            for (t in transactions) {
                val dateStr = DateUtils.formatDate(t.date)
                val escapedNote = "\"${t.note.replace("\"", "\"\"")}\""
                writer.append("${t.id},${t.type.name},\"${t.category}\",${t.amount},${t.paymentMethod.name},$dateStr,$escapedNote\n")
            }
            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "Hisab Khata Transactions CSV")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportDebtsToCsv(context: Context, debts: List<DebtWithPayments>, currencySymbol: String = "৳") {
        try {
            val fileName = "HisabKhata_Debts_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("Person Name,Phone,Type,Total Amount,Paid Amount,Remaining Amount,Status,Date,Due Date,Note\n")
            for (d in debts) {
                val dateStr = DateUtils.formatDate(d.debt.date)
                val dueDateStr = d.debt.dueDate?.let { DateUtils.formatDate(it) } ?: "N/A"
                val typeStr = if (d.debt.debtType.name == "RECEIVABLE") "পাওনা (Receivable)" else "দেনা (Payable)"
                val escapedNote = "\"${d.debt.note.replace("\"", "\"\"")}\""
                writer.append("\"${d.debt.personName}\",${d.debt.phoneNumber},$typeStr,${d.debt.totalAmount},${d.totalPaid},${d.remainingAmount},${d.status.name},$dateStr,$dueDateStr,$escapedNote\n")
            }
            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "Hisab Khata Debts CSV")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportReportSummary(
        context: Context,
        totalIncome: Double,
        totalExpense: Double,
        balance: Double,
        totalReceivable: Double,
        totalPayable: Double,
        transactions: List<TransactionEntity>,
        currencySymbol: String = "৳"
    ) {
        val report = buildString {
            append("═══════════════════════════════════════\n")
            append("           HISAB KHATA FINANCIAL REPORT\n")
            append("═══════════════════════════════════════\n")
            append("Date Generated: ${DateUtils.formatDateTime(System.currentTimeMillis())}\n\n")
            append("FINANCIAL SUMMARY:\n")
            append("---------------------------------------\n")
            append("Current Balance : ${CurrencyUtils.formatAmount(balance, currencySymbol)}\n")
            append("Total Income    : ${CurrencyUtils.formatAmount(totalIncome, currencySymbol)}\n")
            append("Total Expense   : ${CurrencyUtils.formatAmount(totalExpense, currencySymbol)}\n")
            append("Total Receivable: ${CurrencyUtils.formatAmount(totalReceivable, currencySymbol)} (পাওনা)\n")
            append("Total Payable   : ${CurrencyUtils.formatAmount(totalPayable, currencySymbol)} (দেনা)\n\n")
            append("RECENT TRANSACTIONS:\n")
            append("---------------------------------------\n")
            transactions.take(20).forEach { tx ->
                val typeSign = if (tx.type == TransactionType.INCOME) "(+)" else "(-)"
                append("${DateUtils.formatDate(tx.date)} | ${tx.category} | $typeSign ${CurrencyUtils.formatAmount(tx.amount, currencySymbol)} | [${tx.paymentMethod.name}]\n")
                if (tx.note.isNotBlank()) append("   Note: ${tx.note}\n")
            }
            append("\n═══════════════════════════════════════\n")
            append("Generated by Hisab Khata App\n")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Hisab Khata Financial Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Financial Report"))
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            // Fallback plain share
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, file.readText())
            }
            context.startActivity(Intent.createChooser(intent, title))
        }
    }
}
