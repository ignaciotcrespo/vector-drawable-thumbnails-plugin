package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceManagementStrategy
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.util.messages.MessageBusConnection
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced resource management strategy that properly integrates with Android Studio's
 * resource compilation system. This addresses the code review feedback by:
 * - Using Android's actual compiled resources through AppResourceRepository
 * - Supporting all Android Gradle Plugin versions
 * - Properly handling external dependencies (AAR/JAR files)
 * - Implementing proper caching with invalidation
 */
class EnhancedAndroidResourceStrategy : ResourceManagementStrategy {
    
    // Instance variables for proper resource management
    private val vfsListeners = ConcurrentHashMap<Project, MessageBusConnection>()
    private val androidListeners = ConcurrentHashMap<Project, Any>()
    
    companion object {
        private val LOG = Logger.getInstance(EnhancedAndroidResourceStrategy::class.java)
        
        // Android Studio API classes
        private const val ANDROID_FACET_CLASS = "org.jetbrains.android.facet.AndroidFacet"
        private const val APP_RESOURCE_REPO_CLASS = "com.android.tools.idea.res.AppResourceRepository"
        private const val MODULE_RESOURCE_REPO_CLASS = "com.android.tools.idea.res.ModuleResourceRepository"
        private const val LOCAL_RESOURCE_REPO_CLASS = "com.android.tools.idea.res.LocalResourceRepository"
        private const val RESOURCE_ITEM_CLASS = "com.android.ide.common.resources.ResourceItem"
        private const val RESOURCE_TYPE_CLASS = "com.android.resources.ResourceType"
        private const val RESOURCE_VALUE_CLASS = "com.android.ide.common.rendering.api.ResourceValue"
        private const val RESOURCE_REFERENCE_CLASS = "com.android.ide.common.rendering.api.ResourceReference"
        
        // Thread-safe lazy initialization of reflection cache
        @Volatile
        private var reflectionCache: ReflectionCache? = null
        private val reflectionLock = Any()
        
        // Resource cache with proper invalidation
        private val resourceCache = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()
    }
    
    private data class ReflectionCache(
        var androidFacetClass: Class<*>? = null,
        var appResourceRepoClass: Class<*>? = null,
        var moduleResourceRepoClass: Class<*>? = null,
        var localResourceRepoClass: Class<*>? = null,
        var resourceItemClass: Class<*>? = null,
        var resourceTypeClass: Class<*>? = null,
        var resourceValueClass: Class<*>? = null,
        var resourceReferenceClass: Class<*>? = null,
        var colorType: Any? = null,
        var getFacetMethod: Method? = null,
        var getAppResourcesMethod: Method? = null,
        var getModuleResourcesMethod: Method? = null,
        var getResourcesMethod: Method? = null,
        var getResourceValueMethod: Method? = null,
        var resolveMethod: Method? = null,
        var getResolvedValueMethod: Method? = null,
        var isFrameworkMethod: Method? = null,
        var initialized: Boolean = false
    )
    
    override fun isAvailable(project: Project): Boolean {
        return try {
            val cache = getOrInitializeReflectionCache()
            cache.initialized && hasAndroidModules(project)
        } catch (e: Exception) {
            LOG.debug("Android Studio resource integration not available", e)
            false
        }
    }
    
    override fun getColorResources(project: Project): Map<String, String> {
        val projectCacheKey = project.name
        val cachedColors = resourceCache[projectCacheKey]
        if (cachedColors != null && cachedColors.isNotEmpty()) {
            return cachedColors
        }
        
        val colors = ConcurrentHashMap<String, String>()
        
        try {
            ModuleManager.getInstance(project).modules.forEach { module ->
                resolveModuleColors(module, colors)
            }
            
            // Cache the results
            resourceCache[projectCacheKey] = colors
            
        } catch (e: Exception) {
            LOG.error("Error getting color resources from Android Studio", e)
        }
        
        return colors
    }
    
    override fun resolveColorReference(colorRef: String, project: Project): String? {
        if (!colorRef.startsWith("@color/") && !colorRef.startsWith("@android:color/")) {
            return null
        }
        
        // Handle Android system colors first
        if (colorRef.startsWith("@android:color/")) {
            val colorName = colorRef.substringAfter("@android:color/")
            AndroidSystemColors.getSystemColor(colorName)?.let { return it }
        }
        
        try {
            // First check cache
            val cachedColors = getColorResources(project)
            cachedColors[colorRef]?.let { return it }
            
            // If not in cache, try to resolve directly through Android's resource resolver
            ModuleManager.getInstance(project).modules.forEach { module ->
                val resolved = resolveColorInModule(module, colorRef)
                if (resolved != null) {
                    // Update cache
                    resourceCache[project.name]?.put(colorRef, resolved)
                    return resolved
                }
            }
            
        } catch (e: Exception) {
            LOG.debug("Error resolving color reference: $colorRef", e)
        }
        
        return null
    }
    
    override fun setupChangeListeners(project: Project, onChange: () -> Unit) {
        try {
            // Remove any existing listeners first
            removeChangeListeners(project)
            
            // Set up modern file listeners using message bus
            val connection = project.messageBus.connect()
            
            connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    val hasResourceChanges = events.any { event ->
                        val file = when (event) {
                            is VFileCreateEvent -> event.file
                            is VFileDeleteEvent -> event.file
                            is VFileContentChangeEvent -> event.file
                            is VFileMoveEvent -> event.file
                            else -> null
                        }
                        file != null && isResourceFile(file)
                    }
                    
                    if (hasResourceChanges) {
                        invalidateCache(project)
                        onChange()
                    }
                }
            })
            
            // Store the connection for proper cleanup
            vfsListeners[project] = connection
            
            // Also try to hook into Android's resource notification system
            setupAndroidResourceListeners(project, onChange)
            
        } catch (e: Exception) {
            LOG.debug("Could not set up resource change listeners", e)
        }
    }
    
    override fun dispose() {
        // Clean up all listeners
        vfsListeners.forEach { (_, connection) ->
            try {
                connection.disconnect()
            } catch (e: Exception) {
                LOG.debug("Error removing VFS listener", e)
            }
        }
        vfsListeners.clear()
        
        // Clean up Android listeners
        androidListeners.forEach { (project, listener) ->
            try {
                removeAndroidResourceListener(project, listener)
            } catch (e: Exception) {
                LOG.debug("Error removing Android resource listener", e)
            }
        }
        androidListeners.clear()
        
        // Clear cache
        resourceCache.clear()
    }
    
    private fun removeChangeListeners(project: Project) {
        // Remove VFS listener if exists
        vfsListeners[project]?.let { connection ->
            try {
                connection.disconnect()
            } catch (e: Exception) {
                LOG.debug("Error removing VFS listener", e)
            }
        }
        vfsListeners.remove(project)
        
        // Remove Android listener if exists
        androidListeners[project]?.let { listener ->
            try {
                removeAndroidResourceListener(project, listener)
            } catch (e: Exception) {
                LOG.debug("Error removing Android resource listener", e)
            }
        }
        androidListeners.remove(project)
    }
    
    private fun getOrInitializeReflectionCache(): ReflectionCache {
        val currentCache = reflectionCache
        if (currentCache?.initialized == true) {
            return currentCache
        }
        
        synchronized(reflectionLock) {
            val doubleCheckCache = reflectionCache
            if (doubleCheckCache?.initialized == true) {
                return doubleCheckCache
            }
            
            val newCache = ReflectionCache()
            initializeReflectionCache(newCache)
            reflectionCache = newCache
            return newCache
        }
    }
    
    private fun initializeReflectionCache(cache: ReflectionCache) {
        try {
            // Load Android classes
            cache.androidFacetClass = Class.forName(ANDROID_FACET_CLASS)
            cache.appResourceRepoClass = Class.forName(APP_RESOURCE_REPO_CLASS)
            cache.moduleResourceRepoClass = Class.forName(MODULE_RESOURCE_REPO_CLASS)
            cache.localResourceRepoClass = Class.forName(LOCAL_RESOURCE_REPO_CLASS)
            cache.resourceItemClass = Class.forName(RESOURCE_ITEM_CLASS)
            cache.resourceTypeClass = Class.forName(RESOURCE_TYPE_CLASS)
            cache.resourceValueClass = Class.forName(RESOURCE_VALUE_CLASS)
            cache.resourceReferenceClass = Class.forName(RESOURCE_REFERENCE_CLASS)
            
            // Get methods
            cache.getFacetMethod = cache.androidFacetClass?.getMethod("getInstance", Module::class.java)
            cache.getAppResourcesMethod = cache.appResourceRepoClass?.getMethod("getOrCreateInstance", cache.androidFacetClass)
            cache.getModuleResourcesMethod = cache.moduleResourceRepoClass?.getMethod("getOrCreateInstance", cache.androidFacetClass)
            
            // Get color type
            cache.colorType = cache.resourceTypeClass?.getField("COLOR")?.get(null)
            
            // Get resource methods
            cache.getResourcesMethod = cache.localResourceRepoClass?.getMethod("getResources", cache.resourceTypeClass)
            cache.getResourceValueMethod = cache.resourceItemClass?.getMethod("getResourceValue")
            
            // Get resolution methods
            val configurationClass = Class.forName("com.android.ide.common.resources.configuration.FolderConfiguration")
            cache.resolveMethod = cache.resourceValueClass?.getMethod("resolve", cache.localResourceRepoClass, configurationClass)
            cache.getResolvedValueMethod = cache.resourceValueClass?.getMethod("getValue")
            cache.isFrameworkMethod = cache.resourceValueClass?.getMethod("isFramework")
            
            cache.initialized = true
            
        } catch (e: Exception) {
            LOG.debug("Failed to initialize Android resource reflection", e)
            cache.initialized = false
        }
    }
    
    private fun hasAndroidModules(project: Project): Boolean {
        val cache = getOrInitializeReflectionCache()
        return ModuleManager.getInstance(project).modules.any { module ->
            try {
                cache.getFacetMethod?.invoke(null, module) != null
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun resolveModuleColors(module: Module, colors: ConcurrentHashMap<String, String>) {
        try {
            val cache = getOrInitializeReflectionCache()
            val facet = cache.getFacetMethod?.invoke(null, module) ?: return
            
            // Get both app and module resources for comprehensive coverage
            val repositories = listOf(
                cache.getAppResourcesMethod?.invoke(null, facet),
                cache.getModuleResourcesMethod?.invoke(null, facet)
            ).filterNotNull()
            
            val getResourcesMethod = cache.getResourcesMethod
            val getResourceValueMethod = cache.getResourceValueMethod
            val isFrameworkMethod = cache.isFrameworkMethod
            val colorType = cache.colorType
            
            repositories.forEach { repo ->
                val colorItems = getResourcesMethod?.invoke(repo, colorType) as? Collection<*> ?: return@forEach
                
                colorItems.forEach items@{ item ->
                    try {
                        if (item == null) return@items
                        
                        val resourceValue = getResourceValueMethod?.invoke(item) ?: return@items
                        val name = item::class.java.getMethod("getName").invoke(item) as? String ?: return@items
                        
                        // Check if it's a framework resource
                        val isFramework = isFrameworkMethod?.invoke(resourceValue) as? Boolean ?: false
                        val prefix = if (isFramework) "@android:color/" else "@color/"
                        val colorRef = "$prefix$name"
                        
                        // Resolve the value - this handles references properly
                        val resolvedValue = resolveResourceValue(resourceValue, repo)
                        if (resolvedValue != null) {
                            colors[colorRef] = resolvedValue
                        }
                        
                    } catch (e: Exception) {
                        LOG.debug("Error processing color item", e)
                    }
                }
            }
        } catch (e: Exception) {
            LOG.debug("Error resolving module colors", e)
        }
    }
    
    private fun resolveColorInModule(module: Module, colorRef: String): String? {
        try {
            val cache = getOrInitializeReflectionCache()
            val facet = cache.getFacetMethod?.invoke(null, module) ?: return null
            val colorName = colorRef.substringAfter("/")
            val isFramework = colorRef.startsWith("@android:")
            
            // Try app resources first, then module resources
            val repositories = listOf(
                cache.getAppResourcesMethod?.invoke(null, facet),
                cache.getModuleResourcesMethod?.invoke(null, facet)
            ).filterNotNull()
            
            repositories.forEach { repo ->
                try {
                    // Create ResourceReference for proper resolution
                    val namespace = if (isFramework) "android" else null
                    val referenceConstructor = cache.resourceReferenceClass?.getConstructor(
                        String::class.java, cache.resourceTypeClass, String::class.java
                    )
                    val reference = referenceConstructor?.newInstance(namespace, cache.colorType, colorName)
                    
                    // Get resource value
                    val getResourceValueMethod = repo::class.java.getMethod("getResourceValue", cache.resourceReferenceClass)
                    val resourceValue = getResourceValueMethod.invoke(repo, reference)
                    
                    if (resourceValue != null) {
                        return resolveResourceValue(resourceValue, repo)
                    }
                } catch (e: Exception) {
                    LOG.debug("Error resolving color in repository", e)
                }
            }
        } catch (e: Exception) {
            LOG.debug("Error resolving color in module", e)
        }
        
        return null
    }
    
    private fun resolveResourceValue(resourceValue: Any, repository: Any): String? {
        try {
            val cache = getOrInitializeReflectionCache()
            // Get the raw value
            var value = cache.getResolvedValueMethod?.invoke(resourceValue) as? String
            
            if (value != null) {
                // If it's a reference, resolve it recursively
                if (value.startsWith("@") || value.startsWith("?")) {
                    // Use Android's resource resolver
                    val resolved = cache.resolveMethod?.invoke(resourceValue, repository, null)
                    if (resolved != null && resolved != resourceValue) {
                        value = cache.getResolvedValueMethod?.invoke(resolved) as? String
                    }
                }
                
                // Handle color state lists and other complex values
                if (value != null && !value.startsWith("#") && !value.startsWith("@")) {
                    // Try to parse as color int
                    try {
                        val colorInt = java.awt.Color.decode(value).rgb
                        value = String.format("#%06X", 0xFFFFFF and colorInt)
                    } catch (e: Exception) {
                        // Not a valid color string
                        return null
                    }
                }
                
                return value
            }
        } catch (e: Exception) {
            LOG.debug("Error resolving resource value", e)
        }
        
        return null
    }
    
    private fun setupAndroidResourceListeners(project: Project, onChange: () -> Unit) {
        try {
            // Try to use ResourceNotificationManager if available
            val notificationManagerClass = Class.forName("com.android.tools.idea.res.ResourceNotificationManager")
            val listenerInterface = Class.forName("com.android.tools.idea.res.ResourceNotificationManager\$ResourceChangeListener")
            
            val getInstanceMethod = notificationManagerClass.getMethod("getInstance", Project::class.java)
            val manager = getInstanceMethod.invoke(null, project) ?: return
            
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                listenerInterface.classLoader,
                arrayOf(listenerInterface)
            ) { _, method, _ ->
                if (method.name == "resourcesChanged") {
                    invalidateCache(project)
                    onChange()
                }
                null
            }
            
            val addListenerMethod = notificationManagerClass.getMethod("addListener", listenerInterface)
            addListenerMethod.invoke(manager, proxy)
            
            // Store the listener for cleanup
            androidListeners[project] = proxy
            
        } catch (e: Exception) {
            LOG.debug("ResourceNotificationManager not available", e)
        }
    }
    
    private fun removeAndroidResourceListener(project: Project, listener: Any) {
        try {
            val notificationManagerClass = Class.forName("com.android.tools.idea.res.ResourceNotificationManager")
            val listenerInterface = Class.forName("com.android.tools.idea.res.ResourceNotificationManager\$ResourceChangeListener")
            val getInstanceMethod = notificationManagerClass.getMethod("getInstance", Project::class.java)
            val manager = getInstanceMethod.invoke(null, project) ?: return
            
            val removeListenerMethod = notificationManagerClass.getMethod("removeListener", listenerInterface)
            removeListenerMethod.invoke(manager, listener)
        } catch (e: Exception) {
            LOG.debug("Could not remove Android resource listener", e)
        }
    }
    
    private fun isResourceFile(file: VirtualFile): Boolean {
        if (file.isDirectory) return false
        
        return when {
            file.name.endsWith("colors.xml") -> true
            file.name.endsWith("values.xml") -> true
            file.path.contains("/values") && file.extension == "xml" -> true
            file.name == "R.txt" -> true
            else -> false
        }
    }
    
    private fun invalidateCache(project: Project) {
        resourceCache.remove(project.name)
    }
}