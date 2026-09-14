package main.budgieapp

object GoalIcons {
    val options = listOf(
        Triple("savings", "Savings", R.drawable.baseline_savings_24),
        Triple("house", "House", R.drawable.houseicon),
        Triple("vehicle", "Vehicle", R.drawable.baseline_directions_car_24),
        Triple("holiday", "Holiday", R.drawable.holidayicon),
        Triple("education", "Education", R.drawable.baseline_school_24),
        Triple("health", "Healthcare", R.drawable.baseline_health_and_safety_24)
    )

    fun drawableFor(key: String): Int = options.firstOrNull() {
        it.first == key
    }?.third ?: R.drawable.baseline_savings_24
}