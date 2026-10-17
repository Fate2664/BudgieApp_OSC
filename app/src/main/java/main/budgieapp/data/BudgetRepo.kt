package main.budgieapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import main.budgieapp.Category
import java.util.Locale
import java.util.UUID

class BudgetRepo private constructor(private val uid: String) {

    private val auth = FirebaseAuth.getInstance()
    private val budgets =
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("budgets")

    private fun requireCurrentUser() {
        check(auth.currentUser?.uid == uid) {
            "The signed-in account has changed"
        }
    }

    suspend fun getAll(): List<BudgetEntity> {
        requireCurrentUser()

        return budgets.get().await().documents.map { document ->
            requireNotNull(document.toObject(BudgetEntity::class.java))
                .copy(id = document.id)
        }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    suspend fun findForCategory(categoryId: String, period: String): BudgetEntity? {
        requireCurrentUser()
        val document = budgets.document(budgetId(categoryId, period))

        FirebaseFirestore.getInstance().runTransaction
        {
            transaction -> val existing = transaction.get(document)
            if (existing.exists()){
                error("This category already has a ${budget.period} budget")
            }
            transaction.set(document, budget.copy(id = document.id))
        }.await()
    }

    suspend fun deleteById(id: String){
        requireCurrentUser()
        budgets.document(id).delete().await()
    }

    companion object {
        fun forCurrentUser(): BudgetRepo {
            val uid = requireNotNull(FirebaseAuth.getInstance().currentUser)
            {
                "Sign in before accessing budgets"
            }.uid
            return BudgetRepo(uid)
        }

        private fun budgetId(categoryId: String, period: String):String {
            val key = "${categoryId.length}:$categoryId$period"
            return UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8)).toString()
        }
    }
}