package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

/**
 * Extension of ColorResourceResolver that supports configuration.
 * This allows for customizable behavior in color resolution.
 */
interface ConfigurableColorResourceResolver : ColorResourceResolver {
    /**
     * Gets the current configuration for color resolution.
     */
    fun getConfiguration(): ColorResolutionConfig
    
    /**
     * Updates the configuration for color resolution.
     * 
     * @param config The new configuration to apply
     */
    fun setConfiguration(config: ColorResolutionConfig)
}