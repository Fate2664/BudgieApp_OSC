package main.budgieapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class TransactionRepo private constructor(private val uid: String) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val transactions = firestore
        .collection("users")
        .document(uid)
        .collection("transactions")

    private fun requireCurrentUser() {
        check(auth.currentUser?.uid == uid) {
            "The signed-in account has changed"
        }
    }

    private fun decode(document: DocumentSnapshot): TransactionEntity {
        return requireNotNull(document.toObject(TransactionEntity::class.java)) {
            "Could not read transaction ${document.id}"
        }.copy(id = document.id)
    }

    suspend fun getAll(): List<TransactionEntity> {
        requireCurrentUser()
        val snapshot = transactions
            .orderBy("dateTimeMillis", Query.Direction.DESCENDING)
            .get(Source.SERVER)
            .await()
        requireCurrentUser()
        return snapshot.documents.map(::decode)
    }

    suspend fun getByType(type: String): List<TransactionEntity> {
        requireCurrentUser()
        val snapshot = transactions.whereEqualTo("type", type)
            .get(Source.SERVER)
            .await()
        requireCurrentUser()
        return snapshot.documents.map(::decode).sortedByDescending { it.dateTimeMillis }
    }

    suspend fun getInRange(startMillis: Long, endMillis: Long): List<TransactionEntity> {
        requireCurrentUser()
        require(startMillis <= endMillis)

        val snapshot = transactions
            .whereGreaterThanOrEqualTo("dateTimeMillis", startMillis)
            .whereLessThanOrEqualTo("dateTimeMillis", endMillis)
            .orderBy("dateTimeMillis", Query.Direction.ASCENDING)
            .get(Source.SERVER)
            .await()

        requireCurrentUser()
        return snapshot.documents.map(::decode)
    }

    suspend fun insertIfAbsent(transaction: TransactionEntity): Boolean {
        requireCurrentUser()
        require(transaction.id.isNotBlank())
        require('/' !in transaction.id)
        require(transaction.amountCents > 0L)
        require(transaction.type in setOf("INCOME", "EXPENSE"))

        val goalId = transaction.goalId

        if (goalId != null) {
            require(goalId.isNotBlank())
            require('/' !in goalId)
            require(transaction.type == "EXPENSE")
        }

        val reference = transactions.document(transaction.id)
        val goalReference = goalId?.let { id ->
            firestore
                .collection("users")
                .document(uid)
                .collection("goals")
                .document(id)
        }

        val created = firestore.runTransaction { operation ->
            val existing = operation.get(reference)
            if (existing.exists()) {
                false
            } else {
                if (goalReference != null && !operation.get(goalReference).exists()) {
                    throw GoalNotFoundException()
                }
                operation.set(reference, transaction)
                true
            }
        }.await()

        requireCurrentUser()
        return created
    }

    suspend fun deleteAll(): Int {
        requireCurrentUser()
        val documents = transactions.get(Source.SERVER).await().documents
        var deleted = 0
        for (chunk in documents.chunked(200)) {
            requireCurrentUser()
            val batch = firestore.batch()
            for (document in chunk) {
                batch.delete(document.reference)
            }
            batch.commit().await()
            requireCurrentUser()
            deleted += chunk.size
        }
        return deleted
    }

    suspend fun insertSample(samples: List<TransactionEntity>) {
        requireCurrentUser()
        if (samples.isEmpty()) return

        val uniqueSamples = samples.distinctBy { it.id }
        for (sample in uniqueSamples) {
            require(sample.id.isNotBlank())
            require('/' !in sample.id)
            require(sample.amountCents > 0L)
            require(sample.type in setOf("INCOME", "EXPENSE"))
            require(sample.goalId == null) {
                "Sample transactions must not reference goals"
            }
        }

        val existingIds = mutableListOf<String>()

        for (chunk in uniqueSamples.chunked(10)){
            requireCurrentUser()
            val snapshot = transactions
                .whereIn(FieldPath.documentId(), chunk.map { it.id })
                .get(Source.SERVER)
                .await()

            requireCurrentUser()
            existingIds.addAll(snapshot.documents.map { it.id })
        }

        val missingSamples = uniqueSamples.filter { it.id !in existingIds }
        for (chunk in missingSamples.chunked(200)){
            val batch = firestore.batch()
            for (sample in chunk){
                batch.set(transactions.document(sample.id), sample)
            }
            batch.commit().await()
            requireCurrentUser()
        }
    }

    companion object {
        fun forCurrentUser(): TransactionRepo {
            val uid = requireNotNull(FirebaseAuth.getInstance().currentUser) {
                "Sign in before accessing transactions"
            }.uid
            return TransactionRepo(uid)
        }
    }
}