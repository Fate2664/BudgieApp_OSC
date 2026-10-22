package main.budgieapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class GoalRepo private constructor(private val uid: String) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val goals = firestore
        .collection("users")
        .document(uid)
        .collection("goals")

    private fun requireCurrentUser() {
        check(auth.currentUser?.uid == uid) {
            "The signed-in account has changed"
        }
    }

    suspend fun getAll(): List<GoalEntity> {
        requireCurrentUser()

        val snapshot = goals.orderBy("targetDateMillis", Query.Direction.ASCENDING)
            .get(Source.SERVER)
            .await()

        requireCurrentUser()

        return snapshot.documents.map { document ->
            requireNotNull(document.toObject(GoalEntity::class.java)) {
                "Could not read goal ${document.id}"
            }.copy(id = document.id)
        }
    }

    suspend fun findById(id: String): GoalEntity? {
        requireCurrentUser()
        require(id.isNotBlank())
        require('/' !in id)

        val document = goals.document(id).get(Source.SERVER).await()
        requireCurrentUser()

        return document.toObject(GoalEntity::class.java)?.copy(id = document.id)
    }

    suspend fun insertIfAbsent(goal: GoalEntity): Boolean {
        requireCurrentUser()
        require('/' !in goal.id)
        require(goal.name.isNotBlank())
        require(goal.targetAmountCents > 0L)
        require(goal.savedAmountCents >= 0L)
        require(goal.targetDateMillis > 0L)
        require(goal.iconKey.isNotBlank())

        val reference = goals.document(goal.id)
        val tracking = firestore
            .collection("users")
            .document(uid)
            .collection("achievementTracking")
            .document("summary")

        val created = firestore.runTransaction { operation ->
            requireCurrentUser()

            val existing = operation.get(reference)
            if (existing.exists()) {
                false
            } else {
                val summary = operation.get(tracking)
                val goalsCreated = summary.getLong("goalsCreated") ?: 0L

                operation.set(reference, goal)
                operation.set(
                    tracking,
                    mapOf("goalsCreated" to goalsCreated + 1L),
                    SetOptions.merge()
                )
                true
            }
        }.await()
        requireCurrentUser()
        return created
    }

    suspend fun deleteById(id: String) {
        requireCurrentUser()
        require(id.isNotBlank())
        require('/' !in id)

        goals.document(id).delete().await()
        requireCurrentUser()
    }

    companion object {
        fun forCurrentUser(): GoalRepo {
            val uid = requireNotNull(FirebaseAuth.getInstance().currentUser) {
                "Sign in before accessing goals"
            }.uid
            return GoalRepo(uid)
        }
    }
}