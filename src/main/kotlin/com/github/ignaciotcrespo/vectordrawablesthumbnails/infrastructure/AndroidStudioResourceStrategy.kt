package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceManagementStrategy
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.lang.reflect.Method

/**
 * Resource management strategy that integrates with Android Studio's native resource system.
 * Uses reflection to avoid hard dependency on Android plugin.
 */
class AndroidStudioResourceStrategy : ResourceManagementStrategy {
    
    companion object {
        private val LOG = Logger.getInstance(AndroidStudioResourceStrategy::class.java)
        private val ANDROID_FACET_CLASS = "org.jetbrains.android.facet.AndroidFacet"
        private val APP_RESOURCE_REPO_CLASS = "com.android.tools.idea.res.AppResourceRepository"
        private val RESOURCE_ITEM_CLASS = "com.android.ide.common.resources.ResourceItem"
        private val RESOURCE_TYPE_CLASS = "com.android.resources.ResourceType"
        
        // Cache for reflection lookups
        private var androidFacetClass: Class<*>? = null
        private var getFacetMethod: Method? = null
        private var getAppResourcesMethod: Method? = null
        private var getItemsMethod: Method? = null
        private var getResourceValueMethod: Method? = null
        private var getRawValueMethod: Method? = null
        private var colorTypeField: Any? = null
        private var resourceNotificationManagerClass: Class<*>? = null
        private var addListenerMethod: Method? = null
        
        private val INITIALIZATION_KEY = Key.create<Boolean>("AndroidStudioResourceStrategy.initialized")
    }
    
    override fun isAvailable(project: Project): Boolean {
        return try {
            // Check if Android plugin classes are available
            initializeReflection()
            androidFacetClass != null && hasAndroidModules(project)
        } catch (e: Exception) {
            LOG.debug("Android Studio resource integration not available", e)
            false
        }
    }
    
    override fun getColorResources(project: Project): Map<String, String> {
        val colors = mutableMapOf<String, String>()
        
        try {
            ModuleManager.getInstance(project).modules.forEach { module ->
                val facet = getFacetMethod?.invoke(null, module) ?: return@forEach
                val appResources = getAppResourcesMethod?.invoke(null, facet) ?: return@forEach
                
                // Get color items
                val colorItems = (getItemsMethod?.invoke(appResources, colorTypeField) as? Collection<*>) ?: return@forEach
                
                colorItems.forEach { item ->
                    try {
                        if (item == null) return@forEach
                        val resourceValue = getResourceValueMethod?.invoke(item) ?: return@forEach
                        val rawValue = getRawValueMethod?.invoke(resourceValue) as? String ?: return@forEach
                        val name = item::class.java.getMethod("getName").invoke(item) as? String ?: return@forEach
                        
                        colors["@color/$name"] = resolveValue(rawValue, colors)
                    } catch (e: Exception) {
                        LOG.debug("Error processing color item", e)
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Error getting color resources from Android Studio", e)
        }
        
        return colors
    }
    
    override fun resolveColorReference(colorRef: String, project: Project): String? {
        if (!colorRef.startsWith("@color/")) {
            return null
        }
        
        val colorName = colorRef.substringAfter("@color/")
        
        try {
            ModuleManager.getInstance(project).modules.forEach { module ->
                val facet = getFacetMethod?.invoke(null, module) ?: return@forEach
                val appResources = getAppResourcesMethod?.invoke(null, facet) ?: return@forEach
                
                // Get specific color item
                val hasItemMethod = appResources.javaClass.getMethod("hasResourceItem", 
                    colorTypeField?.javaClass, String::class.java)
                val hasItem = hasItemMethod.invoke(appResources, colorTypeField, colorName) as? Boolean ?: false
                
                if (hasItem) {
                    val getItemMethod = appResources.javaClass.getMethod("getResourceItem", 
                        colorTypeField?.javaClass, String::class.java)
                    val items = getItemMethod.invoke(appResources, colorTypeField, colorName) as? List<*>
                    
                    items?.firstOrNull()?.let { item ->
                        val resourceValue = getResourceValueMethod?.invoke(item) ?: return@let
                        val rawValue = getRawValueMethod?.invoke(resourceValue) as? String ?: return@let
                        return resolveValue(rawValue, getColorResources(project))
                    }
                }
            }
        } catch (e: Exception) {
            LOG.debug("Error resolving color reference: $colorRef", e)
        }
        
        return null
    }
    
    override fun setupChangeListeners(project: Project, onChange: () -> Unit) {
        try {
            if (resourceNotificationManagerClass == null || addListenerMethod == null) {
                return
            }
            
            // Get ResourceNotificationManager instance
            val getInstanceMethod = resourceNotificationManagerClass?.getMethod("getInstance", Project::class.java)
            val manager = getInstanceMethod?.invoke(null, project) ?: return
            
            // Create listener
            val listenerInterface = Class.forName("com.android.tools.idea.res.ResourceNotificationManager\$ResourceChangeListener")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                listenerInterface.classLoader,
                arrayOf(listenerInterface)
            ) { _, method, _ ->
                if (method.name == "resourcesChanged") {
                    onChange()
                }
                null
            }
            
            // Add listener
            addListenerMethod?.invoke(manager, proxy)
            
        } catch (e: Exception) {
            LOG.debug("Could not set up Android resource change listeners", e)
        }
    }
    
    override fun dispose() {
        // Cleanup if needed
    }
    
    private fun initializeReflection() {
        if (androidFacetClass != null) {
            return
        }
        
        try {
            // Load Android facet class
            androidFacetClass = Class.forName(ANDROID_FACET_CLASS)
            getFacetMethod = androidFacetClass?.getMethod("getInstance", 
                Class.forName("com.intellij.openapi.module.Module"))
            
            // Load resource repository classes
            val appRepoClass = Class.forName(APP_RESOURCE_REPO_CLASS)
            getAppResourcesMethod = appRepoClass.getMethod("getOrCreateInstance", androidFacetClass)
            
            // Load resource type
            val resourceTypeClass = Class.forName(RESOURCE_TYPE_CLASS)
            colorTypeField = resourceTypeClass.getField("COLOR").get(null)
            
            // Get items method
            getItemsMethod = appRepoClass.getMethod("getResources", resourceTypeClass)
            
            // Resource value methods
            val resourceItemClass = Class.forName(RESOURCE_ITEM_CLASS)
            getResourceValueMethod = resourceItemClass.getMethod("getResourceValue")
            
            val resourceValueClass = Class.forName("com.android.ide.common.rendering.api.ResourceValue")
            getRawValueMethod = resourceValueClass.getMethod("getValue")
            
            // Try to load notification manager
            try {
                resourceNotificationManagerClass = Class.forName("com.android.tools.idea.res.ResourceNotificationManager")
                val listenerClass = Class.forName("com.android.tools.idea.res.ResourceNotificationManager\$ResourceChangeListener")
                addListenerMethod = resourceNotificationManagerClass?.getMethod("addListener", listenerClass)
            } catch (e: Exception) {
                LOG.debug("ResourceNotificationManager not available", e)
            }
            
            // Initialization complete
            
        } catch (e: Exception) {
            LOG.debug("Failed to initialize Android resource reflection", e)
            androidFacetClass = null
        }
    }
    
    private fun hasAndroidModules(project: Project): Boolean {
        return ModuleManager.getInstance(project).modules.any { module ->
            try {
                getFacetMethod?.invoke(null, module) != null
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun resolveValue(value: String, knownColors: Map<String, String>): String {
        return when {
            value.startsWith("#") -> value
            value.startsWith("@color/") -> knownColors[value] ?: value
            value.startsWith("@android:color/") -> resolveSystemColor(value)
            else -> value
        }
    }
    
    private fun resolveSystemColor(colorRef: String): String {
        // Common Android system colors
        return when (colorRef) {
            "@android:color/black" -> "#000000"
            "@android:color/white" -> "#FFFFFF"
            "@android:color/transparent" -> "#00000000"
            "@android:color/holo_blue_dark" -> "#0099CC"
            "@android:color/holo_blue_light" -> "#33B5E5"
            "@android:color/holo_green_dark" -> "#669900"
            "@android:color/holo_green_light" -> "#99CC00"
            "@android:color/holo_orange_dark" -> "#FF8800"
            "@android:color/holo_orange_light" -> "#FFBB33"
            "@android:color/holo_red_dark" -> "#CC0000"
            "@android:color/holo_red_light" -> "#FF4444"
            else -> "#000000"
        }
    }
    
    private val project: Project? = null
}