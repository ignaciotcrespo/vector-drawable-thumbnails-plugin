package com.github.ignaciotcrespo.vectordrawablesthumbnails.utils

/**
 * Simple performance monitoring utility for tracking operation times.
 * Helps identify performance bottlenecks in the plugin.
 */
object PerformanceMonitor {
    
    private val measurements = mutableMapOf<String, MutableList<Long>>()
    
    /**
     * Measures the execution time of a block of code.
     */
    inline fun <T> measure(operationName: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        try {
            return block()
        } finally {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            recordMeasurement(operationName, duration)
            
            // Log slow operations (> 100ms)
            if (duration > 100) {
                println("⚠️ Slow operation: $operationName took ${duration}ms")
            }
        }
    }
    
    /**
     * Records a measurement for later analysis.
     */
    fun recordMeasurement(operationName: String, duration: Long) {
        measurements.getOrPut(operationName) { mutableListOf() }.add(duration)
    }
    
    /**
     * Gets performance statistics for an operation.
     */
    fun getStats(operationName: String): PerformanceStats? {
        val times = measurements[operationName] ?: return null
        if (times.isEmpty()) return null
        
        return PerformanceStats(
            operationName = operationName,
            count = times.size,
            totalTime = times.sum(),
            averageTime = times.average(),
            minTime = times.minOrNull() ?: 0,
            maxTime = times.maxOrNull() ?: 0
        )
    }
    
    /**
     * Prints performance summary for all measured operations.
     */
    fun printSummary() {
        println("\n📊 Performance Summary:")
        println("=" * 50)
        
        measurements.keys.sorted().forEach { operationName ->
            getStats(operationName)?.let { stats ->
                println("${stats.operationName}:")
                println("  Count: ${stats.count}")
                println("  Total: ${stats.totalTime}ms")
                println("  Average: ${"%.1f".format(stats.averageTime)}ms")
                println("  Min: ${stats.minTime}ms")
                println("  Max: ${stats.maxTime}ms")
                println()
            }
        }
    }
    
    /**
     * Clears all measurements.
     */
    fun clear() {
        measurements.clear()
    }
}

/**
 * Performance statistics for an operation.
 */
data class PerformanceStats(
    val operationName: String,
    val count: Int,
    val totalTime: Long,
    val averageTime: Double,
    val minTime: Long,
    val maxTime: Long
)

/**
 * Extension function for string repetition.
 */
private operator fun String.times(count: Int): String = repeat(count) 