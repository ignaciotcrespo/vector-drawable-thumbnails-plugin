package com.github.ignaciotcrespo.vectordrawablesthumbnails.scanners

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.io.File
import java.nio.file.Files

@ExtendWith(MockitoExtension::class)
class ProjectFileScannerTest {

    @Mock
    lateinit var project: Project
    @Mock
    lateinit var moduleManager: ModuleManager
    @Mock
    lateinit var module: Module
    @Mock
    lateinit var moduleRootManager: ModuleRootManager
    @Mock
    lateinit var localFileSystem: LocalFileSystem
    @Mock
    lateinit var virtualFile: VirtualFile // General mock for non-excluded files/dirs
    @Mock
    lateinit var excludedVirtualFile: VirtualFile // Mock for explicitly excluded VirtualFile

    private lateinit var scanner: ProjectFileScanner
    private lateinit var tempDir: File
    private lateinit var mockStaticModuleManager: MockedStatic<ModuleManager>
    private lateinit var mockStaticModuleRootManager: MockedStatic<ModuleRootManager>
    private lateinit var mockStaticLocalFileSystem: MockedStatic<LocalFileSystem>

    @BeforeEach
    fun setUp() {
        scanner = ProjectFileScanner()
        tempDir = Files.createTempDirectory("scannerTest").toFile()

        // Mock static IntelliJ API calls
        mockStaticModuleManager = Mockito.mockStatic(ModuleManager::class.java)
        mockStaticModuleManager.`when`<Any> { ModuleManager.getInstance(project) }.thenReturn(moduleManager)

        mockStaticModuleRootManager = Mockito.mockStatic(ModuleRootManager::class.java)
        mockStaticModuleRootManager.`when`<Any> { ModuleRootManager.getInstance(module) }.thenReturn(moduleRootManager)

        mockStaticLocalFileSystem = Mockito.mockStatic(LocalFileSystem::class.java)
        mockStaticLocalFileSystem.`when`<Any> { LocalFileSystem.getInstance() }.thenReturn(localFileSystem)

        // Default behaviors for mocks
        whenever(moduleManager.modules).thenReturn(arrayOf(module))
        whenever(project.basePath).thenReturn(tempDir.absolutePath)
        whenever(moduleRootManager.excludeRoots).thenReturn(emptyArray()) // Default to no excluded roots by ModuleRootManager
        whenever(localFileSystem.findFileByIoFile(any())).thenReturn(virtualFile) // Default: all files are found by LFS
        whenever(virtualFile.path).thenReturn("some/generic/path") // Default path
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
        mockStaticModuleManager.close()
        mockStaticModuleRootManager.close()
        mockStaticLocalFileSystem.close()
    }

    private fun setupProjectBasePath(file: File) {
        whenever(project.basePath).thenReturn(file.absolutePath)
    }

    private fun createFileInTempDir(relativePath: String): File {
        val file = File(tempDir, relativePath)
        file.parentFile.mkdirs()
        file.createNewFile()
        return file
    }

    private fun createDirInTempDir(relativePath: String): File {
        val dir = File(tempDir, relativePath)
        dir.mkdirs()
        return dir
    }

    @Test
    fun `should find XML files in simple directory`() {
        // Given
        createFileInTempDir("icon1.xml")
        createFileInTempDir("data.txt")
        setupProjectBasePath(tempDir)

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0) { it.file.name == "icon1.xml" }
        testObserver.assertComplete()
    }

    @Test
    fun `should find XML files in nested directories`() {
        // Given
        createFileInTempDir("root.xml")
        createFileInTempDir("level1/icon2.xml")
        createFileInTempDir("level1/level2/icon3.xml")
        setupProjectBasePath(tempDir)

        // When
        val testObserver: TestObserver<ValidFile> = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(3)
        testObserver.assertValueAt(0) { it.file.name == "root.xml" }
        testObserver.assertValueAt(1) { it.file.name == "icon2.xml" }
        testObserver.assertValueAt(2) { it.file.name == "icon3.xml" }
        testObserver.assertComplete()
    }

    @Test
    fun `should ignore files in dotGradle directory`() {
        // Given
        createFileInTempDir(".gradle/ignored.xml")
        createFileInTempDir("root.xml")
        setupProjectBasePath(tempDir)

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0) { it.file.name == "root.xml" }
        testObserver.assertComplete()
    }

    @Test
    fun `should ignore files in dotIdea directory`() {
        // Given
        createFileInTempDir(".idea/ignored.xml")
        createFileInTempDir("another.xml")
        setupProjectBasePath(tempDir)

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0) { it.file.name == "another.xml" }
        testObserver.assertComplete()
    }
    
    @Test
    fun `should ignore files in build generated directory`() {
        // Given
        createFileInTempDir("build/generated/generated.xml")
        createFileInTempDir("app/src/main/res/layout/activity_main.xml")
        setupProjectBasePath(tempDir)

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0) { it.file.name == "activity_main.xml" }
        testObserver.assertComplete()
    }

    @Test
    fun `should ignore files in build intermediates directory`() {
        // Given
        createFileInTempDir("build/intermediates/inter.xml")
        createFileInTempDir("app/src/main/res/drawable/vector.xml")
        setupProjectBasePath(tempDir)

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0) { it.file.name == "vector.xml" }
        testObserver.assertComplete()
    }


    @Test
    fun `should return empty when no XML files present`() {
        // Given
        createFileInTempDir("file1.txt")
        createFileInTempDir("level1/file2.doc")
        setupProjectBasePath(tempDir)

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }

    @Test
    fun `should return empty for an empty directory`() {
        // Given
        setupProjectBasePath(tempDir) // tempDir is empty by default

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }

    @Test
    fun `should ignore files in explicitly excluded VirtualFile root by ModuleRootManager`() {
        // Given
        val excludedDirFile = createDirInTempDir("excludedDir")
        createFileInTempDir("excludedDir/ignored.xml")
        createFileInTempDir("includedDir/included.xml")
        setupProjectBasePath(tempDir)

        // Mock LocalFileSystem to return specific VirtualFile for the excluded directory
        whenever(localFileSystem.findFileByIoFile(excludedDirFile)).thenReturn(excludedVirtualFile)
        // Mock the excluded VirtualFile to have a specific path (optional, but good for clarity)
        whenever(excludedVirtualFile.path).thenReturn(excludedDirFile.absolutePath)
        // Configure ModuleRootManager to return this VirtualFile as an excluded root
        whenever(moduleRootManager.excludeRoots).thenReturn(arrayOf(excludedVirtualFile))


        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0) { it.file.name == "included.xml" }
        testObserver.assertComplete()
    }
    
    @Test
    fun `should handle project with no modules`() {
        // Given
        whenever(moduleManager.modules).thenReturn(emptyArray())
        setupProjectBasePath(tempDir)
        createFileInTempDir("somefile.xml") // This file should not be found as no modules means no scan path

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0) // Expect no files as the iteration over modules won't happen
        testObserver.assertComplete()
    }

    @Test
    fun `should handle project base path being null`() {
        // Given
        whenever(project.basePath).thenReturn(null)
        // No files created, as basePath is null, scan shouldn't proceed to file system search

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0) // Expect no files
        testObserver.assertComplete()
    }
    
    @Test
    fun `should correctly identify projectRootFolder for ValidFile`() {
        // Given
        val xmlFile = createFileInTempDir("myModule/src/main/res/icon.xml")
        setupProjectBasePath(tempDir) // tempDir is the project root

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0) {
            it.file.name == "icon.xml" && it.projectRootFolder == tempDir.absolutePath
        }
        testObserver.assertComplete()
    }

    @Test
    fun `should ignore VirtualFile that cannot be found by LocalFileSystem`() {
        // Given
        val dirToMakeLFSFail = createDirInTempDir("problematicDir")
        createFileInTempDir("problematicDir/some.xml") // File that might be skipped
        createFileInTempDir("goodDir/good.xml") // File that should be found
        setupProjectBasePath(tempDir)

        // Make LocalFileSystem return null for the "problematicDir"
        whenever(localFileSystem.findFileByIoFile(dirToMakeLFSFail)).thenReturn(null)
        // All other directories/files will return the default 'virtualFile' mock

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
        testObserver.assertNoErrors()
        // It should find "good.xml".
        // The behavior for "problematicDir/some.xml" is that if its parent VirtualFile is null,
        // it's not considered excluded, so it *should* be found by the file walk.
        // The exclusion check `if (fVirtual != null) { for (excluded in excludedRoots) ... }`
        // means if fVirtual is null, it's NOT excluded by the ModuleRootManager list.
        testObserver.assertValueCount(2)
        testObserver.assertComplete()
    }
}

// Helper to allow mocking static ModuleRootManager.getInstance(module)
// This is a common pattern for mocking static methods with specific arguments.
// For ModuleManager.getInstance(project) it's simpler as project is already a mock.
private fun <T> MockedStatic<ModuleManager>.`when`(function: () -> T): MockedStatic.Verification {
    return this.`when`(function)
}
private fun <T> MockedStatic<ModuleRootManager>.`when`(function: () -> T): MockedStatic.Verification {
    return this.`when`(function)
}
private fun <T> MockedStatic<LocalFileSystem>.`when`(function: () -> T): MockedStatic.Verification {
    return this.`when`(function)
}
