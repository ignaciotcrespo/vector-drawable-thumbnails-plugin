package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.intellij.openapi.vfs.VirtualFile

/**
 * Interface for parsing resource files to extract color definitions.
 * Follows Single Responsibility Principle - only responsible for parsing.
 */
interface ResourceFileParser {
    /**
     * Parses a resource file and extracts color definitions.
     * 
     * @param file The file to parse
     * @return Map of color names to their values (hex or references)
     */
    fun parseResourceFile(file: VirtualFile): Map<String, String>
    
    /**
     * Parses R.txt file format to extract color resource IDs.
     * 
     * @param file The R.txt file to parse
     * @return Set of color resource names found
     */
    fun parseRTxtFile(file: VirtualFile): Set<String>
}