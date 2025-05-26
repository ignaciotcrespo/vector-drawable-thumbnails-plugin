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

    @Test
    fun `scanner should handle null project basePath gracefully`() {
        // Given
        Mockito.lenient().`when`(project.basePath).thenReturn(null)

        // When
        val testObserver = scanner.findXmlFiles(project).test()

        // Then
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

        // Then
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

        // Then
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }
} 