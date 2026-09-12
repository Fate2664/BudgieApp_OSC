package main.budgieapp.data


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
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

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view){
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
        holder.amount.setTextColor(holder.itemView.context.getColor(
            if (transaction.type == TransactionType.INCOME.name){
                R.color.light_green
            } else {
                R.color.light_red
            }
        ))
        holder.attachment.apply {
            visibility = if (transaction.attachmentUri.isNullOrBlank()){
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            setImageResource(android.R.drawable.ic_menu_gallery)
            isClickable = false
            isFocusable = false
            contentDescription = "Has attachment"
        }
    }
}