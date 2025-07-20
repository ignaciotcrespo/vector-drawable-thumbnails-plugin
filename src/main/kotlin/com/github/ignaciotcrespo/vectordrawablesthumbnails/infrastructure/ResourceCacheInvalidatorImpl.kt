package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceCacheInvalidator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.openapi.vfs.VirtualFilePropertyEvent
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.messages.MessageBusConnection

/**
 * Implementation of ResourceCacheInvalidator that watches for file changes
 * and triggers cache invalidation when resource files are modified.
 */
class ResourceCacheInvalidatorImpl : ResourceCacheInvalidator {
    
    companion object {
        private val LOG = Logger.getInstance(ResourceCacheInvalidatorImpl::class.java)
        
        // Patterns for resource files that should trigger cache invalidation
        private val RESOURCE_FILE_PATTERNS = listOf(
            Regex(".*/values/.*\\.xml$"),
            Regex(".*/values-.*/.*\\.xml$"),
            Regex(".*/R\\.txt$"),
            Regex(".*/colors\\.xml$"),
            Regex(".*/build/.*/values/.*\\.xml$")
        )
    }
    
    private var messageBusConnection: MessageBusConnection? = null
    private val invalidationCallbacks = mutableListOf<() -> Unit>()
    private var lastInvalidationTime = 0L
    private val invalidationDebounceMs = 500L // Debounce rapid file changes
    
    override fun startWatching(project: Project) {
        stopWatching() // Ensure we don't have duplicate listeners
        
        try {
            messageBusConnection = project.messageBus.connect()
            
            // Use BulkFileListener for better performance
            messageBusConnection?.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    handleFileEvents(events)
                }
            })
            
            LOG.info("Started watching for resource file changes")
            
        } catch (e: Exception) {
            LOG.error("Failed to start file watcher", e)
        }
    }
    
    override fun stopWatching() {
        try {
            messageBusConnection?.disconnect()
            messageBusConnection = null
            LOG.info("Stopped watching for resource file changes")
        } catch (e: Exception) {
            LOG.error("Error stopping file watcher", e)
        }
    }
    
    override fun onInvalidate(callback: () -> Unit) {
        invalidationCallbacks.add(callback)
    }
    
    override fun isResourceFile(filePath: String): Boolean {
        return RESOURCE_FILE_PATTERNS.any { pattern ->
            pattern.matches(filePath)
        }
    }
    
    private fun handleFileEvents(events: List<VFileEvent>) {
        val relevantEvents = events.filter { event ->
            when (event) {
                is VFileContentChangeEvent -> isResourceFile(event.file.path)
                is VFileCreateEvent -> isResourceFile(event.path)
                is VFileDeleteEvent -> isResourceFile(event.file.path)
                is VFileMoveEvent -> isResourceFile(event.oldPath) || isResourceFile(event.newPath)
                is VFilePropertyChangeEvent -> {
                    event.propertyName == "name" && 
                    (isResourceFile(event.oldPath) || isResourceFile(event.newPath))
                }
                else -> false
            }
        }
        
        if (relevantEvents.isNotEmpty()) {
            LOG.debug("Detected ${relevantEvents.size} resource file changes")
            triggerInvalidation()
        }
    }
    
    private fun triggerInvalidation() {
        val currentTime = System.currentTimeMillis()
        
        // Debounce rapid changes
        if (currentTime - lastInvalidationTime < invalidationDebounceMs) {
            return
        }
        
        lastInvalidationTime = currentTime
        
        // Notify all callbacks
        invalidationCallbacks.forEach { callback ->
            try {
                callback()
            } catch (e: Exception) {
                LOG.error("Error in invalidation callback", e)
            }
        }
    }
}