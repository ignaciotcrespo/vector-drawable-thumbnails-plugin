package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.cache

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.AndroidResourceLocator
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceEntry
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceFileCollection
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceFileParser
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for ColorCacheBuilder to verify proper cache building
 * with concurrent processing and error handling.
 */
class ColorCacheBuilderTest {
    
    @Mock
    private lateinit var mockAndroidResourceLocator: AndroidResourceLocator
    
    @Mock
    private lateinit var mockResourceParser: ResourceFileParser
    
    @Mock
    private lateinit var mockProject: Project
    
    @Mock
    private lateinit var mockProgressIndicator: ProgressIndicator
    
    private lateinit var cacheBuilder: ColorCacheBuilder
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        cacheBuilder = ColorCacheBuilder(mockAndroidResourceLocator, mockResourceParser)
    }
    
    @Test
    fun `test buildColorCache returns empty map when no resources found`() = runBlocking {
        // Given
        val emptyCollection = ResourceFileCollection(emptyList(), emptyList(), emptyList())
        whenever(mockAndroidResourceLocator.findAllResourceFiles(mockProject)).thenReturn(emptyCollection)
        
        // When
        val result = cacheBuilder.buildColorCache(mockProject)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `test buildColorCache processes source files`() = runBlocking {
        // Given
        val mockFile = mock<VirtualFile> {
            on { name } doReturn "colors.xml"
            on { extension } doReturn "xml"
            on { path } doReturn "/src/main/res/values/colors.xml"
        }
        
        val colors = mapOf("primary" to "#FF0000", "secondary" to "#00FF00")
        whenever(mockResourceParser.parseResourceFile(mockFile)).thenReturn(colors)
        
        val collection = ResourceFileCollection(listOf(mockFile), emptyList(), emptyList())
        whenever(mockAndroidResourceLocator.findAllResourceFiles(mockProject)).thenReturn(collection)
        
        // When
        val result = cacheBuilder.buildColorCache(mockProject)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("#FF0000", result["primary"])
        assertEquals("#00FF00", result["secondary"])
    }
    
    @Test
    fun `test buildColorCache prioritizes build output over source files`() = runBlocking {
        // Given
        val mockSourceFile = mock<VirtualFile> {
            on { name } doReturn "colors.xml"
            on { extension } doReturn "xml"
            on { path } doReturn "/src/main/res/values/colors.xml"
        }
        
        val mockBuildFile = mock<VirtualFile> {
            on { name } doReturn "colors.xml"
            on { extension } doReturn "xml"
            on { path } doReturn "/build/intermediates/res/merged/debug/values/colors.xml"
        }
        
        val sourceColors = mapOf("primary" to "#FF0000")
        val buildColors = mapOf("primary" to "#0000FF") // Different color value
        
        whenever(mockResourceParser.parseResourceFile(mockSourceFile)).thenReturn(sourceColors)
        whenever(mockResourceParser.parseResourceFile(mockBuildFile)).thenReturn(buildColors)
        
        val collection = ResourceFileCollection(
            listOf(mockSourceFile),
            listOf(mockBuildFile),
            emptyList()
        )
        whenever(mockAndroidResourceLocator.findAllResourceFiles(mockProject)).thenReturn(collection)
        
        // When
        val result = cacheBuilder.buildColorCache(mockProject)
        
        // Then
        assertEquals(1, result.size)
        assertEquals("#0000FF", result["primary"]) // Build output should win
    }
    
    @Test
    fun `test buildColorCache processes R txt files`() = runBlocking {
        // Given
        val mockRTxtFile = mock<VirtualFile> {
            on { name } doReturn "R.txt"
            on { extension } doReturn "txt"
        }
        
        val colorNames = setOf("primary_color", "secondary_color")
        whenever(mockResourceParser.parseRTxtFile(mockRTxtFile)).thenReturn(colorNames)
        
        val collection = ResourceFileCollection(listOf(mockRTxtFile), emptyList(), emptyList())
        whenever(mockAndroidResourceLocator.findAllResourceFiles(mockProject)).thenReturn(collection)
        
        // When
        val result = cacheBuilder.buildColorCache(mockProject)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("@color/primary_color", result["primary_color"])
        assertEquals("@color/secondary_color", result["secondary_color"])
    }
    
    @Test
    fun `test buildColorCache processes library entries`() = runBlocking {
        // Given
        val xmlEntry = ResourceEntry(
            path = "aar:support-v7.aar!/res/values/colors.xml",
            content = """
                <resources>
                    <color name="material_blue">#2196F3</color>
                </resources>
            """.trimIndent()
        )
        
        val rTxtEntry = ResourceEntry(
            path = "aar:support-v7.aar!/R.txt",
            content = "int color accent_material_light 0x7f060001"
        )
        
        val collection = ResourceFileCollection(
            emptyList(),
            emptyList(),
            listOf(xmlEntry, rTxtEntry)
        )
        whenever(mockAndroidResourceLocator.findAllResourceFiles(mockProject)).thenReturn(collection)
        
        // When
        val result = cacheBuilder.buildColorCache(mockProject)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("#2196F3", result["material_blue"])
        assertEquals("@color/accent_material_light", result["accent_material_light"])
    }
    
    @Test
    fun `test buildColorCache handles errors gracefully`() = runBlocking {
        // Given
        val mockFile = mock<VirtualFile> {
            on { name } doReturn "colors.xml"
            on { extension } doReturn "xml"
        }
        
        whenever(mockResourceParser.parseResourceFile(mockFile))
            .thenThrow(RuntimeException("Parse error"))
        
        val collection = ResourceFileCollection(listOf(mockFile), emptyList(), emptyList())
        whenever(mockAndroidResourceLocator.findAllResourceFiles(mockProject)).thenReturn(collection)
        
        // When
        val result = cacheBuilder.buildColorCache(mockProject)
        
        // Then
        assertTrue(result.isEmpty()) // Should handle error and continue
    }
    
    @Test
    fun `test buildColorCacheWithProgress updates progress indicator`() = runBlocking {
        // Given
        val mockFile1 = mock<VirtualFile> {
            on { name } doReturn "colors1.xml"
            on { extension } doReturn "xml"
        }
        val mockFile2 = mock<VirtualFile> {
            on { name } doReturn "colors2.xml"
            on { extension } doReturn "xml"
        }
        
        whenever(mockResourceParser.parseResourceFile(any())).thenReturn(mapOf("test" to "#000000"))
        
        val collection = ResourceFileCollection(
            listOf(mockFile1, mockFile2),
            emptyList(),
            emptyList()
        )
        whenever(mockAndroidResourceLocator.findAllResourceFiles(mockProject)).thenReturn(collection)
        
        // When
        val result = cacheBuilder.buildColorCacheWithProgress(mockProject, mockProgressIndicator)
        
        // Then
        verify(mockProgressIndicator).text = "Locating Android resource files..."
        verify(mockProgressIndicator).isIndeterminate = true
        verify(mockProgressIndicator).text = "Parsing 2 resource items..."
        verify(mockProgressIndicator).isIndeterminate = false
        verify(mockProgressIndicator, atLeastOnce()).fraction = any()
        verify(mockProgressIndicator, atLeastOnce()).text2 = any()
        verify(mockProgressIndicator).text = "Completed. Found 1 color resources"
        
        assertEquals(1, result.size)
    }
    
    @Test
    fun `test buildColorCacheWithProgress handles empty resources`() = runBlocking {
        // Given
        val emptyCollection = ResourceFileCollection(emptyList(), emptyList(), emptyList())
        whenever(mockAndroidResourceLocator.findAllResourceFiles(mockProject)).thenReturn(emptyCollection)
        
        // When
        val result = cacheBuilder.buildColorCacheWithProgress(mockProject, mockProgressIndicator)
        
        // Then
        verify(mockProgressIndicator).text = "No Android resource files found"
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `test concurrent processing of multiple files`() = runBlocking {
        // Given
        val fileCount = 10
        val mockFiles = (1..fileCount).map { i ->
            mock<VirtualFile> {
                on { name } doReturn "colors$i.xml"
                on { extension } doReturn "xml"
                on { path } doReturn "/res/values/colors$i.xml"
            }
        }
        
        mockFiles.forEachIndexed { index, file ->
            whenever(mockResourceParser.parseResourceFile(file))
                .thenReturn(mapOf("color$index" to "#00000$index"))
        }
        
        val collection = ResourceFileCollection(mockFiles, emptyList(), emptyList())
        whenever(mockAndroidResourceLocator.findAllResourceFiles(mockProject)).thenReturn(collection)
        
        // When
        val result = cacheBuilder.buildColorCache(mockProject)
        
        // Then
        assertEquals(fileCount, result.size)
        (0 until fileCount).forEach { i ->
            assertEquals("#00000$i", result["color$i"])
        }
    }
}