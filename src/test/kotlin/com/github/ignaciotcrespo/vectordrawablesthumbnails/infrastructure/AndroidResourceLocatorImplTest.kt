package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidResourceLocatorImplTest {
    
    @Mock
    private lateinit var mockProject: Project
    
    @Mock
    private lateinit var mockModuleManager: ModuleManager
    
    @Mock
    private lateinit var mockModule: Module
    
    @Mock
    private lateinit var mockModuleRootManager: ModuleRootManager
    
    @Mock
    private lateinit var mockSourceRoot: VirtualFile
    
    @Mock
    private lateinit var mockModuleFile: VirtualFile
    
    @Mock
    private lateinit var mockModuleDir: VirtualFile
    
    private lateinit var locator: AndroidResourceLocatorImpl
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        locator = AndroidResourceLocatorImpl()
        
        // Setup basic mocks
        whenever(ModuleManager.getInstance(mockProject)).thenReturn(mockModuleManager)
        whenever(mockModuleManager.modules).thenReturn(arrayOf(mockModule))
        whenever(ModuleRootManager.getInstance(mockModule)).thenReturn(mockModuleRootManager)
    }
    
    @Test
    fun `test findAllResourceFiles returns empty collection when no modules`() {
        whenever(mockModuleManager.modules).thenReturn(emptyArray())
        
        val result = locator.findAllResourceFiles(mockProject)
        
        assertTrue(result.sourceFiles.isEmpty())
        assertTrue(result.buildOutputFiles.isEmpty())
        assertTrue(result.libraryEntries.isEmpty())
    }
    
    @Test
    fun `test findAllResourceFiles finds source resource files`() {
        // Setup source roots
        val mockColorsFile = mock<VirtualFile> {
            on { name } doReturn "colors.xml"
            on { exists() } doReturn true
            on { isDirectory } doReturn false
            on { parent } doReturn mock<VirtualFile> {
                on { name } doReturn "values"
            }
        }
        
        val mockValuesDir = mock<VirtualFile> {
            on { name } doReturn "values"
            on { exists() } doReturn true
            on { isDirectory } doReturn true
            on { children } doReturn arrayOf(mockColorsFile)
        }
        
        whenever(mockSourceRoot.exists()).thenReturn(true)
        whenever(mockSourceRoot.isDirectory).thenReturn(true)
        whenever(mockSourceRoot.children).thenReturn(arrayOf(mockValuesDir))
        whenever(mockModuleRootManager.sourceRoots).thenReturn(arrayOf(mockSourceRoot))
        
        val result = locator.findAllResourceFiles(mockProject)
        
        assertEquals(1, result.sourceFiles.size)
        assertEquals("colors.xml", result.sourceFiles.first().name)
    }
    
    @Test
    fun `test getSupportedBuildVariants returns default variants when none found`() {
        whenever(mockModule.moduleFile).thenReturn(null)
        
        val result = locator.getSupportedBuildVariants(mockProject)
        
        assertEquals(listOf("debug", "release"), result)
    }
    
    @Test
    fun `test getSupportedBuildVariants detects build variants from directories`() {
        val mockDebugDir = mock<VirtualFile> {
            on { name } doReturn "mergeDebugResources"
            on { isDirectory } doReturn true
        }
        
        val mockReleaseDir = mock<VirtualFile> {
            on { name } doReturn "mergeReleaseResources"
            on { isDirectory } doReturn true
        }
        
        val mockIntermediatesDir = mock<VirtualFile> {
            on { name } doReturn "intermediates"
            on { isDirectory } doReturn true
            on { children } doReturn arrayOf(mockDebugDir, mockReleaseDir)
        }
        
        val mockBuildDir = mock<VirtualFile> {
            on { exists() } doReturn true
            on { isDirectory } doReturn true
            on { children } doReturn arrayOf(mockIntermediatesDir)
        }
        
        whenever(mockModule.moduleFile).thenReturn(mockModuleFile)
        whenever(mockModuleFile.parent).thenReturn(mockModuleDir)
        whenever(mockModuleDir.findFileByRelativePath("build")).thenReturn(mockBuildDir)
        
        val result = locator.getSupportedBuildVariants(mockProject)
        
        assertTrue(result.contains("debug"))
        assertTrue(result.contains("release"))
    }
    
    @Test
    fun `test findAllResourceFiles handles R txt files`() {
        val mockRTxtFile = mock<VirtualFile> {
            on { name } doReturn "R.txt"
            on { exists() } doReturn true
            on { isDirectory } doReturn false
        }
        
        whenever(mockSourceRoot.exists()).thenReturn(true)
        whenever(mockSourceRoot.isDirectory).thenReturn(true)
        whenever(mockSourceRoot.children).thenReturn(arrayOf(mockRTxtFile))
        whenever(mockModuleRootManager.sourceRoots).thenReturn(arrayOf(mockSourceRoot))
        
        val result = locator.findAllResourceFiles(mockProject)
        
        assertEquals(1, result.sourceFiles.size)
        assertEquals("R.txt", result.sourceFiles.first().name)
    }
    
    @Test
    fun `test findAllResourceFiles skips non-resource directories`() {
        val mockDrawableDir = mock<VirtualFile> {
            on { name } doReturn "drawable"
            on { exists() } doReturn true
            on { isDirectory } doReturn true
        }
        
        val mockLayoutDir = mock<VirtualFile> {
            on { name } doReturn "layout"
            on { exists() } doReturn true
            on { isDirectory } doReturn true
        }
        
        whenever(mockSourceRoot.exists()).thenReturn(true)
        whenever(mockSourceRoot.isDirectory).thenReturn(true)
        whenever(mockSourceRoot.children).thenReturn(arrayOf(mockDrawableDir, mockLayoutDir))
        whenever(mockModuleRootManager.sourceRoots).thenReturn(arrayOf(mockSourceRoot))
        
        val result = locator.findAllResourceFiles(mockProject)
        
        assertTrue(result.sourceFiles.isEmpty())
    }
}