package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.ByteArrayInputStream
import java.io.File

class ProjectVectorFileProviderTest {

    private lateinit var projectVectorFileProvider: ProjectVectorFileProvider
    private lateinit var mockProject: Project
    private lateinit var mockModuleManager: ModuleManager
    private lateinit var mockModule: Module
    private lateinit var mockModuleRootManager: ModuleRootManager
    private lateinit var mockVirtualFile: VirtualFile
    private lateinit var mockLocalFileSystem: LocalFileSystem

    @BeforeEach
    fun setUp() {
        projectVectorFileProvider = ProjectVectorFileProvider()
        mockProject = mock()
        mockModuleManager = mock()
        mockModule = mock()
        mockModuleRootManager = mock()
        mockVirtualFile = mock()
        mockLocalFileSystem = mock()

        whenever(mockProject.getComponent(ModuleManager::class.java)).thenReturn(mockModuleManager)
        whenever(mockModuleManager.modules).thenReturn(arrayOf(mockModule))
        whenever(mockModule.getComponent(ModuleRootManager::class.java)).thenReturn(mockModuleRootManager)

        // Mock LocalFileSystem.getInstance()
        // This is a bit tricky as it's a static method.
        // For a real project, consider using PowerMockito or refactoring to inject LocalFileSystem.
        // For this example, we'll assume it can be mocked or we test logic that doesn't directly call it.
        // Or, ensure searchFilesRecursively is testable by passing a file system abstraction.
        // For now, we will mock the direct calls if possible, or accept limitations.
    }

    private fun mockSourceRoots(vararg roots: VirtualFile) {
        whenever(mockModuleRootManager.sourceRoots).thenReturn(roots)
    }

    private fun mockVirtualFile(
        name: String,
        path: String,
        isDirectory: Boolean,
        extension: String? = null,
        children: Array<VirtualFile>? = null,
        content: String = ""
    ): VirtualFile {
        val file: VirtualFile = mock()
        whenever(file.name).thenReturn(name)
        whenever(file.path).thenReturn(path)
        whenever(file.isDirectory).thenReturn(isDirectory)
        whenever(file.extension).thenReturn(extension)
        if (isDirectory) {
            whenever(file.children).thenReturn(children ?: emptyArray())
        } else {
            // Mock file content reading for XML check
            val mockFileIo = mock<File>()
            whenever(mockFileIo.readText()).thenReturn(content)
            // This part is tricky because LocalFileSystem.getInstance().findFileByIoFile is hard to mock.
            // We'll assume that if a file is passed to ValidFile constructor, it's found.
            // A deeper refactor of ProjectVectorFileProvider might be needed for full testability here.
            // For now, we focus on the filtering and recursion logic.

            // We need to mock the LocalFileSystem interaction if possible
            // For simplicity, if a VirtualFile represents a file that would be read,
            // we'll assume it's correctly translated to a File object for readText().
            // This requires ProjectVectorFileProvider to be refactored or to use a testable file system wrapper.
            // As a workaround, we can try to mock the file reading part if it becomes a blocker.
            // For now, let's assume the ValidFile creation implies a readable file.
        }
        return file
    }

    @Test
    fun `getValidFilesObservable with no modules`() {
        whenever(mockModuleManager.modules).thenReturn(emptyArray())
        val testObserver = TestObserver<ValidFile>()

        projectVectorFileProvider.getValidFilesObservable(mockProject).subscribe(testObserver)

        testObserver.assertNoErrors()
        testObserver.assertComplete()
        testObserver.assertNoValues()
    }

    @Test
    fun `getValidFilesObservable with modules but no XML files`() {
        val rootDir = mockVirtualFile("root", "/project/root", true, children = arrayOf(
            mockVirtualFile("file.txt", "/project/root/file.txt", false, "txt")
        ))
        mockSourceRoots(rootDir)
        val testObserver = TestObserver<ValidFile>()

        projectVectorFileProvider.getValidFilesObservable(mockProject).subscribe(testObserver)

        testObserver.assertNoErrors()
        testObserver.assertComplete()
        testObserver.assertNoValues()
    }

    @Test
    fun `getValidFilesObservable with modules and one vector XML file`() {
        // We need to ensure that the File(child.path).readText() call can be handled.
        // This is where mocking static LocalFileSystem or refactoring ProjectVectorFileProvider is key.
        // Let's assume for this test that if a .xml file is found, its content check is what we're testing.

        val xmlFile = mockVirtualFile("vector.xml", "/project/res/drawable/vector.xml", false, "xml")
        val resDir = mockVirtualFile("res", "/project/res", true, children = arrayOf(
            mockVirtualFile("drawable", "/project/res/drawable", true, children = arrayOf(xmlFile))
        ))
        val rootDir = mockVirtualFile("src", "/project/src", true, children = arrayOf(resDir))
        mockSourceRoots(rootDir)

        // Mocking file system interaction for the readText() call
        val mockIoFile = mock<File>()
        whenever(mockIoFile.readText()).thenReturn("<vector></vector>")

        // This is the problematic part: static LocalFileSystem.getInstance()
        // We'll bypass direct mocking of it for now and assume ValidFile can be created.
        // To truly test this, ProjectVectorFileProvider would need LocalFileSystem injected or a wrapper.
        val staticMockLocalFileSystem = mockStatic(LocalFileSystem::class)
        whenever(LocalFileSystem.getInstance()).thenReturn(mockLocalFileSystem)
        whenever(mockLocalFileSystem.findFileByIoFile(any())).thenReturn(xmlFile) // Simulate finding the file

        // Create a temporary file with vector content to simulate the File(child.path).readText()
        val tempDir = createTempDir("testDir")
        val tempFile = File(tempDir, "vector.xml")
        tempFile.writeText("<vector android:name=\"vector_name\"></vector>")
        val virtualTempFile = mockVirtualFile("vector.xml", tempFile.absolutePath, false, "xml")

        val drawableDirWithTemp = mockVirtualFile("drawable", "/project/res/drawable", true, children = arrayOf(virtualTempFile))
        val resDirWithTemp = mockVirtualFile("res", "/project/res", true, children = arrayOf(drawableDirWithTemp))
        val rootDirWithTemp = mockVirtualFile("src", "/project/src", true, children = arrayOf(resDirWithTemp))
        mockSourceRoots(rootDirWithTemp)


        val testObserver = TestObserver<ValidFile>()
        projectVectorFileProvider.getValidFilesObservable(mockProject).subscribe(testObserver)

        testObserver.assertNoErrors()
        testObserver.assertComplete()
        testObserver.assertValueCount(1)
        testObserver.assertValue { it.virtualFile.name == "vector.xml" }

        tempFile.delete()
        tempDir.delete()
        staticMockLocalFileSystem.close()
    }

    @Test
    fun `getValidFilesObservable with modules and non-vector XML file`() {
        val xmlFile = mockVirtualFile("regular.xml", "/project/res/layout/regular.xml", false, "xml")
        val layoutDir = mockVirtualFile("layout", "/project/res/layout", true, children = arrayOf(xmlFile))
        val resDir = mockVirtualFile("res", "/project/res", true, children = arrayOf(layoutDir))
        val rootDir = mockVirtualFile("src", "/project/src", true, children = arrayOf(resDir))
        mockSourceRoots(rootDir)

        val tempDir = createTempDir("testDir_nonvector")
        val tempFile = File(tempDir, "regular.xml")
        tempFile.writeText("<layout></layout>") // Not a vector
        val virtualTempFile = mockVirtualFile("regular.xml", tempFile.absolutePath, false, "xml")

        val layoutDirWithTemp = mockVirtualFile("layout", "/project/res/layout", true, children = arrayOf(virtualTempFile))
        val resDirWithTemp = mockVirtualFile("res", "/project/res", true, children = arrayOf(layoutDirWithTemp))
        val rootDirWithTemp = mockVirtualFile("src", "/project/src", true, children = arrayOf(resDirWithTemp))
        mockSourceRoots(rootDirWithTemp)

        val staticMockLocalFileSystem = mockStatic(LocalFileSystem::class)
        whenever(LocalFileSystem.getInstance()).thenReturn(mockLocalFileSystem)
        whenever(mockLocalFileSystem.findFileByIoFile(any())).thenReturn(virtualTempFile)


        val testObserver = TestObserver<ValidFile>()
        projectVectorFileProvider.getValidFilesObservable(mockProject).subscribe(testObserver)

        testObserver.assertNoErrors()
        testObserver.assertComplete()
        testObserver.assertNoValues()

        tempFile.delete()
        tempDir.delete()
        staticMockLocalFileSystem.close()
    }


    @Test
    fun `getValidFilesObservable with excluded directories`() {
        val buildGeneratedFile = mockVirtualFile("vector_gen.xml", "/project/src/build/generated/res/vector_gen.xml", false, "xml")
        val buildDir = mockVirtualFile("build", "/project/src/build", true, children = arrayOf(
            mockVirtualFile("generated", "/project/src/build/generated", true, children = arrayOf(
                mockVirtualFile("res", "/project/src/build/generated/res", true, children = arrayOf(buildGeneratedFile))
            ))
        ))
        val ideaFile = mockVirtualFile("workspace.xml", "/project/.idea/workspace.xml", false, "xml")
        val ideaDir = mockVirtualFile(".idea", "/project/.idea", true, children = arrayOf(ideaFile))

        val regularVectorFile = mockVirtualFile("vector.xml", "/project/src/main/res/drawable/vector.xml", false, "xml")
        val mainResDir = mockVirtualFile("main", "/project/src/main", true, children = arrayOf(
            mockVirtualFile("res", "/project/src/main/res", true, children = arrayOf(
                mockVirtualFile("drawable", "/project/src/main/res/drawable", true, children = arrayOf(regularVectorFile))
            ))
        ))

        val rootDir = mockVirtualFile("src", "/project/src", true, children = arrayOf(buildDir, ideaDir, mainResDir))
        mockSourceRoots(rootDir)


        val tempDir = createTempDir("testDir_excluded")
        val tempFileRegular = File(tempDir, "vector.xml")
        tempFileRegular.writeText("<vector></vector>")
        val virtualTempFileRegular = mockVirtualFile("vector.xml", tempFileRegular.absolutePath, false, "xml")

        // Need to simulate the file path for the excluded files as well, though they shouldn't be read
        val tempFileGenerated = File(tempDir, "vector_gen.xml")
        tempFileGenerated.writeText("<vector></vector>") // Content doesn't matter as it should be excluded by path
        val virtualTempFileGenerated = mockVirtualFile("vector_gen.xml", tempFileGenerated.absolutePath, false, "xml")


        val buildGeneratedResDirWithTemp = mockVirtualFile("res", "/project/src/build/generated/res", true, children = arrayOf(virtualTempFileGenerated))
        val buildGeneratedDirWithTemp = mockVirtualFile("generated", "/project/src/build/generated", true, children = arrayOf(buildGeneratedResDirWithTemp))
        val buildDirWithTemp = mockVirtualFile("build", "/project/src/build", true, children = arrayOf(buildGeneratedDirWithTemp))


        val drawableDirWithRegularTemp = mockVirtualFile("drawable", "/project/src/main/res/drawable", true, children = arrayOf(virtualTempFileRegular))
        val mainResDirWithTemp = mockVirtualFile("res", "/project/src/main/res", true, children = arrayOf(drawableDirWithRegularTemp))
        val mainDirWithTemp = mockVirtualFile("main", "/project/src/main", true, children = arrayOf(mainResDirWithTemp))


        val rootDirWithTemps = mockVirtualFile("src", "/project/src", true, children = arrayOf(buildDirWithTemp, ideaDir, mainDirWithTemp))
        mockSourceRoots(rootDirWithTemps) // Important: use the structure with temp files for path checks

        val staticMockLocalFileSystem = mockStatic(LocalFileSystem::class)
        whenever(LocalFileSystem.getInstance()).thenReturn(mockLocalFileSystem)
        // Simulate finding the regular file, excluded files should not reach this point if path exclusion works
        whenever(mockLocalFileSystem.findFileByIoFile(argThat { it.name == "vector.xml" })).thenReturn(virtualTempFileRegular)
        // For excluded files, findFileByIoFile might be called if the path isn't excluded early enough.
        // The test relies on the path string check in `isExcluded` method.

        val testObserver = TestObserver<ValidFile>()
        projectVectorFileProvider.getValidFilesObservable(mockProject).subscribe(testObserver)

        testObserver.assertNoErrors()
        testObserver.assertComplete()
        testObserver.assertValueCount(1)
        testObserver.assertValue { it.virtualFile.name == "vector.xml" && it.virtualFile.path == tempFileRegular.absolutePath }

        tempFileRegular.delete()
        tempFileGenerated.delete()
        tempDir.delete()
        staticMockLocalFileSystem.close()
    }
}
