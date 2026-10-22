package main.budgieapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import main.budgieapp.Category
import java.util.Locale
import java.util.UUID

class BudgetRepo private constructor(private val uid: String) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
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

        val snapshot = budgets.get(Source.SERVER).await()
        requireCurrentUser()

        return snapshot.documents.map { document ->
            requireNotNull(document.toObject(BudgetEntity::class.java)
            ) {
                "Could not read budget ${document.id}"
            }.copy(id = document.id)
        }.sortedBy {
            it.name.lowercase(Locale.ROOT) }
    }

    suspend fun findForCategory(categoryId: String, period: String): BudgetEntity? {
        requireCurrentUser()
        val document = budgets
            .document(budgetId(categoryId, period))
            .get(Source.SERVER)
            .await()

        requireCurrentUser()

        return document.toObject(BudgetEntity::class.java)
    }

    suspend fun deleteById(id: String){
        requireCurrentUser()
        require(id.isNotBlank())
        require('/' !in id)

        budgets.document(id).delete().await()
        requireCurrentUser()
    }

    suspend fun insert(budget: BudgetEntity): Boolean {
        requireCurrentUser()
        require(budget.name.isNotBlank())
        require(budget.amountCents > 0L)
        require(budget.categoryId.isNotBlank())
        require(budget.period in setOf("Daily", "Weekly", "Monthly", "Yearly"))

        val reference = budgets.document(budgetId(budget.categoryId, budget.period))
        val created = firestore.runTransaction { operation ->
            val existing = operation.get(reference)

            if (existing.exists()) {
                false
            } else {
                operation.set(
                    reference,
                    budget.copy(id = reference.id, createdAtMillis = System.currentTimeMillis())
                )
                true
            }
        }.await()

        requireCurrentUser()
        return created
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