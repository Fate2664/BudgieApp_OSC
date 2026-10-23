package main.budgieapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.rpc.BadRequest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.compareTo
import kotlin.text.compareTo

class AchievementRepo private constructor(private val uid: String) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val zone = ZoneId.of("Africa/Johannesburg")

    private val user = firestore.collection("users").document(uid)
    private val achievements = user.collection("achievements")
    private val tracking = user.collection("achievementTracking").document("summary")

    private fun requireCurrentUser() {
        check(auth.currentUser?.uid == uid) {
            "The signed-in account has changed"
        }
    }

    suspend fun refreshAndGetEarned(forceRefresh: Boolean = false): Set<String> {
        return refreshMutex.withLock {
            requireCurrentUser()

            val cached = synchronized(cache) { cache[uid] }
            if (!forceRefresh && cached != null && android.os.SystemClock.elapsedRealtime() - cached.refreshedAt < CACHE_DURATION_MS) {
                return@withLock cached.earned
            }

            val earned = calculateAndSaveEarned()
            requireCurrentUser()
            synchronized(cache) {
                cache[uid] = CachedAchievements(
                    earned = earned.toSet(),
                    refreshedAt = android.os.SystemClock.elapsedRealtime()
                )
            }
            earned
        }
    }

    // Missing flags belong to legacy achievements and are intentionally silent.
    fun observePendingNotifications(): Flow<Set<String>> = callbackFlow {
        requireCurrentUser()
        val registration = achievements.addSnapshotListener(
            com.google.firebase.firestore.MetadataChanges.INCLUDE
        ) { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (auth.currentUser?.uid != uid) {
                close(IllegalStateException("The signed-in account has changed"))
            } else if (snapshot != null && !snapshot.metadata.isFromCache &&
                !snapshot.metadata.hasPendingWrites()
            ) {
                trySend(snapshot.documents.filter {
                    it.id in ALL_IDS && it.getBoolean("earned") == true &&
                        it.getBoolean("notificationShown") == false
                }.map { it.id }.toSet())
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun isNotificationPending(id: String): Boolean {
        require(id in ALL_IDS)
        requireCurrentUser()
        val snapshot = achievements.document(id).get(Source.SERVER).await()
        requireCurrentUser()
        return snapshot.getBoolean("earned") == true &&
            snapshot.getBoolean("notificationShown") == false
    }

    // Acknowledge only after display so lifecycle cancellation cannot lose a popup.
    suspend fun markNotificationShown(id: String): Boolean {
        require(id in ALL_IDS)
        requireCurrentUser()
        val claimed = firestore.runTransaction { operation ->
            requireCurrentUser()
            val document = achievements.document(id)
            val snapshot = operation.get(document)
            if (snapshot.getBoolean("earned") != true ||
                snapshot.getBoolean("notificationShown") != false
            ) {
                false
            } else {
                operation.update(document, mapOf(
                    "notificationShown" to true,
                    "notificationShownAt" to FieldValue.serverTimestamp()
                ))
                true
            }
        }.await()
        requireCurrentUser()
        return claimed
    }

    private suspend fun calculateAndSaveEarned(): Set<String> = coroutineScope {
        requireCurrentUser()

        val budgetRepo = BudgetRepo.forCurrentUser()
        val goalRepo = GoalRepo.forCurrentUser()
        val transactionRepo = TransactionRepo.forCurrentUser()

        val budgetsDeferred = async { budgetRepo.getAll() }
        val goalsDeferred = async { goalRepo.getAll() }
        val transactionsDeferred = async { transactionRepo.getAll() }

        val budgets = budgetsDeferred.await()
        val goals = goalsDeferred.await()
        val transactions = transactionsDeferred.await()

        requireCurrentUser()

        val now = System.currentTimeMillis()
        val currentMonth = monthOf(now)
        val expenses = transactions.filter { transaction ->
            transaction.type == "EXPENSE" &&
                    transaction.status == "Cleared" &&
                    transaction.amountCents > 0L &&
                    transaction.dateTimeMillis > 0L &&
                    transaction.dateTimeMillis <= now
        }

        val contributions = expenses.filter { !it.goalId.isNullOrBlank() }
        val contributionsByGoal = contributions.groupBy { requireNotNull(it.goalId) }
            .mapValues { (_, entries) -> totalCents(entries) }
        val savingMonths = contributions.map { monthOf(it.dateTimeMillis) }
            .filter { it < currentMonth }
            .toSet()
        val spendingByCategoryAndMonth =
            expenses.groupBy { it.categoryId to monthOf(it.dateTimeMillis) }
                .mapValues { (_, entries) -> totalCents(entries) }

        val eligible = mutableSetOf<String>()

        if (budgets.isNotEmpty()) {
            eligible.add(FIRST_STEP)
        }

        for (goal in goals) {
            if (goal.targetAmountCents <= 0L) {
                continue
            }
            val saved = BigDecimal.valueOf(goal.savedAmountCents)
                .add(contributionsByGoal[goal.id] ?: BigDecimal.ZERO)
            val target = BigDecimal.valueOf(goal.targetAmountCents)

            if (saved.multiply(BigDecimal.valueOf(2L)) >= target) {
                eligible.add(HALFWAY_THERE)
            }

            if (saved >= target) {
                eligible.add(GOAL_ACHIEVED)
            }
        }

        if (hasThreeConsecutiveMonths(savingMonths)) {
            eligible.add(CONSISTENT_SAVER)
        }

        if (budgets.any { budget ->
                qualifiesWithinBudget(budget, spendingByCategoryAndMonth, currentMonth)
            }) {
            eligible.add(WITHIN_BUDGET)
        }

        val earned = firestore.runTransaction { operation ->
            requireCurrentUser()

            val existing = ALL_IDS.associateWith { achievementId ->
                operation.get(achievements.document(achievementId))
            }
            val summary = operation.get(tracking)
            val storedGoalCount = summary.getLong("goalsCreated") ?: 0L
            val goalCount = maxOf(storedGoalCount, goals.size.toLong())
            val eligibleNow = eligible.toMutableSet()

            if (goalCount >= 3L) {
                eligibleNow.add(GOAL_EXPLORER)
            }

            val previouslyEarned = existing.filterValues { it.exists() }.keys

            requireCurrentUser()

            if (!summary.exists() || goalCount != storedGoalCount) {
                operation.set(tracking, mapOf("goalsCreated" to goalCount), SetOptions.merge())
            }

            for (achievementId in eligibleNow - previouslyEarned) {
                operation.set(
                    achievements.document(achievementId),
                    mapOf(
                        "earned" to true,
                        "earnedAt" to FieldValue.serverTimestamp(),
                        "notificationShown" to false
                    )
                )
            }

            previouslyEarned + eligibleNow
        }.await()

        requireCurrentUser()
        earned
    }

    fun getCachedEarned(): Set<String>?{
        requireCurrentUser()
        return synchronized(cache) { cache[uid]?.earned }
    }

    private fun monthOf(timestamp: Long): YearMonth {
        return YearMonth.from(Instant.ofEpochMilli(timestamp).atZone(zone))
    }

    private fun totalCents(transactions: List<TransactionEntity>): BigDecimal {
        return transactions.fold(BigDecimal.ZERO) { total, transaction ->
            total.add(BigDecimal.valueOf(transaction.amountCents))
        }
    }

    private fun hasThreeConsecutiveMonths(months: Set<YearMonth>): Boolean {
        return months.any { month ->
            month.plusMonths(1L) in months &&
                    month.plusMonths(2L) in months
        }
    }

    private fun qualifiesWithinBudget(
        budget: BudgetEntity,
        spending: Map<Pair<String, YearMonth>, BigDecimal>,
        currentMonth: YearMonth
    ): Boolean {
        if (budget.period != "Monthly" ||
            budget.amountCents <= 0L ||
            budget.createdAtMillis <= 0L
        ) {
            return false
        }

        var month = monthOf(budget.createdAtMillis).plusMonths(1L)
        var consecutiveMonths = 0
        val limit = BigDecimal.valueOf(budget.amountCents)

        while (month < currentMonth) {
            val spent = spending[budget.categoryId to month] ?: BigDecimal.ZERO
            consecutiveMonths = if (spent <= limit) {
                consecutiveMonths + 1
            } else {
                0
            }

            if (consecutiveMonths >= 3) {
                return true
            }

            month = month.plusMonths(1L)
        }
        return false
    }

    companion object {
        const val FIRST_STEP = "first_step"
        const val HALFWAY_THERE = "halfway_there"
        const val GOAL_ACHIEVED = "goal_achieved"
        const val CONSISTENT_SAVER = "consistent_saver"
        const val WITHIN_BUDGET = "within_budget"
        const val GOAL_EXPLORER = "goal_explorer"
        private const val CACHE_DURATION_MS = 30_000L

        val ALL_IDS = listOf(
            FIRST_STEP,
            HALFWAY_THERE,
            GOAL_ACHIEVED,
            CONSISTENT_SAVER,
            WITHIN_BUDGET,
            GOAL_EXPLORER
        )

        fun forCurrentUser(): AchievementRepo {
            val uid = requireNotNull(FirebaseAuth.getInstance().currentUser) {
                "Sign in before accessing achievements"
            }.uid
            return AchievementRepo(uid)
        }

        private data class CachedAchievements(
            val earned: Set<String>,
            val refreshedAt: Long
        )

        private val cache = mutableMapOf<String, CachedAchievements>()
        private val refreshMutex = Mutex()
    }
}
