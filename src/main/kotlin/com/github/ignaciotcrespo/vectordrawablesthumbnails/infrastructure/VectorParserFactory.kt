package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResourceResolver
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorParser
import com.intellij.openapi.diagnostic.Logger

/**
 * Factory for creating VectorParser instances.
 * This factory allows switching between different parser implementations
 * based on configuration or runtime conditions.
 */
object VectorParserFactory {
    
    private val LOG = Logger.getInstance(VectorParserFactory::class.java)
    
    /**
     * Creates a VectorParser instance based on configuration.
     * By default, uses the AsyncVectorParser for better performance.
     * Falls back to DefaultVectorParser if async mode is disabled or fails.
     */
    fun createParser(colorResourceResolver: ColorResourceResolver): VectorParser {
        return try {
            // Check if async mode is disabled via system property
            val asyncDisabled = System.getProperty("vector.parser.async.disabled", "false").toBoolean()
            
            if (asyncDisabled) {
                LOG.info("Async vector parsing disabled, using synchronous parser")
                DefaultVectorParser(colorResourceResolver)
            } else {
                LOG.info("Using async vector parser for improved performance")
                AsyncVectorParser(colorResourceResolver)
            }
        } catch (e: Exception) {
            LOG.error("Failed to create async parser, falling back to default", e)
            DefaultVectorParser(colorResourceResolver)
        }
    }
}