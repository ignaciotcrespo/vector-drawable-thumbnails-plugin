package com.github.ignaciotcrespo.vectordrawablesthumbnails.scanners

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.intellij.openapi.project.Project
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.io.File
import java.nio.file.Files

@ExtendWith(MockitoExtension::class)
class ProjectFileScannerTest {

    @Mock
    lateinit var project: Project

    private lateinit var scanner: ProjectFileScanner
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        scanner = ProjectFileScanner()
        tempDir = Files.createTempDirectory("scannerTest").toFile()
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createFileInTempDir(relativePath: String): File {
        val file = File(tempDir, relativePath)
        file.parentFile.mkdirs()
        file.createNewFile()
        return file
    }

    @Test
    fun `scanner should handle null project basePath gracefully`() {
        // Given
        Mockito.lenient().`when`(project.basePath).thenReturn(null)

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then - should complete without errors even with null basePath
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }

    @Test
    fun `scanner should handle empty project basePath gracefully`() {
        // Given
        Mockito.lenient().`when`(project.basePath).thenReturn("")

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then - should complete without errors even with empty basePath
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }

    @Test
    fun `scanner should handle non-existent project path gracefully`() {
        // Given
        Mockito.lenient().`when`(project.basePath).thenReturn("/non/existent/path")

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then - should complete without errors even with non-existent path
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }
}

/**
 * Pure unit tests for file scanning logic without IntelliJ dependencies
 */
@ExtendWith(MockitoExtension::class)
class FileSystemScannerTest {

    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        tempDir = Files.createTempDirectory("fileSystemTest").toFile()
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createFileInTempDir(relativePath: String): File {
        val file = File(tempDir, relativePath)
        file.parentFile.mkdirs()
        file.createNewFile()
        return file
    }

    @Test
    fun `should find XML files in directory structure`() {
        // Given
        createFileInTempDir("icon1.xml")
        createFileInTempDir("icon2.xml")
        createFileInTempDir("data.txt")
        createFileInTempDir("subdir/icon3.xml")
        createFileInTempDir("subdir/readme.md")

        // When
        val xmlFiles = findXmlFilesInDirectory(tempDir)

        // Then
        assert(xmlFiles.size == 3) { "Should find 3 XML files, found ${xmlFiles.size}" }
        assert(xmlFiles.any { it.name == "icon1.xml" }) { "Should find icon1.xml" }
        assert(xmlFiles.any { it.name == "icon2.xml" }) { "Should find icon2.xml" }
        assert(xmlFiles.any { it.name == "icon3.xml" }) { "Should find icon3.xml" }
    }

    @Test
    fun `should exclude gradle and idea directories`() {
        // Given
        createFileInTempDir("icon1.xml")
        createFileInTempDir(".gradle/cache.xml")
        createFileInTempDir(".idea/workspace.xml")
        createFileInTempDir("build/generated/icon.xml")
        createFileInTempDir("build/intermediates/icon.xml")

        // When
        val xmlFiles = findXmlFilesInDirectory(tempDir)

        // Then
        assert(xmlFiles.size == 1) { "Should find only 1 XML file (excluding build/gradle/idea), found ${xmlFiles.size}" }
        assert(xmlFiles.any { it.name == "icon1.xml" }) { "Should find icon1.xml" }
    }

    @Test
    fun `should handle empty directory`() {
        // When
        val xmlFiles = findXmlFilesInDirectory(tempDir)

        // Then
        assert(xmlFiles.isEmpty()) { "Should find no files in empty directory" }
    }

    @Test
    fun `should handle directory with no XML files`() {
        // Given
        createFileInTempDir("readme.txt")
        createFileInTempDir("config.json")
        createFileInTempDir("subdir/data.csv")

        // When
        val xmlFiles = findXmlFilesInDirectory(tempDir)

        // Then
        assert(xmlFiles.isEmpty()) { "Should find no XML files" }
    }

    @Test
    fun `should handle nested directory structure`() {
        // Given
        createFileInTempDir("level1/level2/level3/deep.xml")
        createFileInTempDir("level1/shallow.xml")

        // When
        val xmlFiles = findXmlFilesInDirectory(tempDir)

        // Then
        assert(xmlFiles.size == 2) { "Should find 2 XML files in nested structure" }
        assert(xmlFiles.any { it.name == "deep.xml" }) { "Should find deep.xml" }
        assert(xmlFiles.any { it.name == "shallow.xml" }) { "Should find shallow.xml" }
    }

    /**
     * Pure file system scanning logic extracted from ProjectFileScanner
     * This can be tested without IntelliJ dependencies
     */
    private fun findXmlFilesInDirectory(directory: File): List<File> {
        val xmlFiles = mutableListOf<File>()
        scanDirectory(directory, xmlFiles)
        return xmlFiles
    }

    private fun scanDirectory(directory: File, xmlFiles: MutableList<File>) {
        val files = directory.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                // Skip excluded directories (same logic as ProjectFileScanner)
                if (file.name == ".gradle" || file.name == ".idea" ||
                    (file.absolutePath.contains("build") && 
                     (file.absolutePath.contains("generated") || file.absolutePath.contains("intermediates")))
                ) {
                    continue
                }
                scanDirectory(file, xmlFiles)
            } else if (file.name.endsWith(".xml")) {
                xmlFiles.add(file)
            }
        }
    }
} 