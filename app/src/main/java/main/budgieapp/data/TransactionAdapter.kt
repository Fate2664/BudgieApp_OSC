package main.budgieapp.data


import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import main.budgieapp.R
import main.budgieapp.TransactionType
import main.budgieapp.data.TransactionEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(private val transactions: List<TransactionEntity>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.txtTransactionDate)
        val category: TextView = view.findViewById(R.id.txtTransactionCategory)
        val payee: TextView = view.findViewById(R.id.txtTransactionPayee)
        val amount: TextView = view.findViewById(R.id.txtTransactionAmount)
        val attachment: ImageButton = view.findViewById(R.id.btnAttachment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)

        return TransactionViewHolder(view)
    }

    override fun getItemCount(): Int = transactions.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.date.text = dateFormat.format(Date(transaction.dateTimeMillis))
        holder.category.text = transaction.categoryName
        holder.payee.text = transaction.payee
        holder.amount.text = currencyFormat.format(BigDecimal.valueOf(transaction.amountCents, 2))
        holder.amount.setTextColor(
            holder.itemView.context.getColor(
                if (transaction.type == TransactionType.INCOME.name) {
                    R.color.light_green
                } else {
                    R.color.light_red
                }
            )
        )
        holder.attachment.apply {
            val attachmentUri = transaction.attachmentUri
            val hasAttachment = !attachmentUri.isNullOrBlank()

            visibility = if (hasAttachment) View.VISIBLE else
                View.INVISIBLE
            setImageResource(android.R.drawable.ic_menu_gallery)
            contentDescription = "Open attachment"

            setOnClickListener(null)

            if (hasAttachment) {
                setOnClickListener {
                    val context = holder.itemView.context
                    val uri = Uri.parse(attachmentUri!!)

                    try {
                        val mimeType = context.contentResolver.getType(uri) ?: "*/*"
                        val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }

                        context.startActivity(openIntent)
                    } catch (exception: ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            "No app is installed that can open this attachment.",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (exception: SecurityException) {
                        Toast.makeText(
                            context,
                            "Access to this attachment is no longer available.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            isClickable = hasAttachment
            isFocusable = hasAttachment
        }
    }
}