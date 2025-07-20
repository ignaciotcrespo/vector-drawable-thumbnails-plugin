package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResourceResolver
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.intellij.testFramework.LightPlatformTestCase
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.kotlin.*
import java.io.File

/**
 * Test for DefaultVectorParser to verify color resolution in vector files
 */
class DefaultVectorParserTest : LightPlatformTestCase() {
    
    private lateinit var mockColorResolver: ColorResourceResolver
    private lateinit var parser: DefaultVectorParser
    
    override fun setUp() {
        super.setUp()
        mockColorResolver = mock()
        parser = DefaultVectorParser(mockColorResolver)
    }
    
    @Test
    fun testParseVectorWithColorReferences() {
        // Create a temporary vector file with color references
        val vectorContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24"
                android:viewportHeight="24">
                <path
                    android:fillColor="@color/primary"
                    android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z"/>
            </vector>
        """.trimIndent()
        
        val tempFile = createTempFile("test_vector", ".xml")
        tempFile.writeText(vectorContent)
        
        // Mock color resolution
        whenever(mockColorResolver.resolveColorReference("@color/primary", project))
            .thenReturn("#FF0000")
        
        // Create ValidFile
        val validFile = ValidFile(tempFile, tempFile.parent)
        
        // Parse vector
        val testObserver = TestObserver<com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem>()
        parser.parseVectorFile(validFile, project).subscribe(testObserver)
        
        // Verify
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        
        val vectorItem = testObserver.values()[0]
        assertEquals("test_vector.xml", vectorItem.name)
        assertEquals(24, vectorItem.viewportW)
        assertEquals(24, vectorItem.viewportH)
        
        // Verify color was resolved
        verify(mockColorResolver).resolveColorReference("@color/primary", project)
        
        // Clean up
        tempFile.delete()
    }
    
    @Test
    fun testParseVectorWithAndroidSystemColors() {
        // Create a temporary vector file with Android system colors
        val vectorContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24"
                android:viewportHeight="24">
                <path
                    android:fillColor="@android:color/black"
                    android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z"/>
            </vector>
        """.trimIndent()
        
        val tempFile = createTempFile("test_vector_android", ".xml")
        tempFile.writeText(vectorContent)
        
        // Mock color resolution
        whenever(mockColorResolver.resolveColorReference("@android:color/black", project))
            .thenReturn("#000000")
        
        // Create ValidFile
        val validFile = ValidFile(tempFile, tempFile.parent)
        
        // Parse vector
        val testObserver = TestObserver<com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem>()
        parser.parseVectorFile(validFile, project).subscribe(testObserver)
        
        // Verify
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        
        // Verify Android color was resolved
        verify(mockColorResolver).resolveColorReference("@android:color/black", project)
        
        // Clean up
        tempFile.delete()
    }
    
    @Test
    fun testParseVectorWithMultipleColorReferences() {
        // Create a vector with multiple color references
        val vectorContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24"
                android:viewportHeight="24">
                <path
                    android:fillColor="@color/primary"
                    android:strokeColor="@color/secondary"
                    android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z"/>
            </vector>
        """.trimIndent()
        
        val tempFile = createTempFile("test_vector_multi", ".xml")
        tempFile.writeText(vectorContent)
        
        // Mock color resolutions
        whenever(mockColorResolver.resolveColorReference("@color/primary", project))
            .thenReturn("#FF0000")
        whenever(mockColorResolver.resolveColorReference("@color/secondary", project))
            .thenReturn("#00FF00")
        
        // Create ValidFile
        val validFile = ValidFile(tempFile, tempFile.parent)
        
        // Parse vector
        val testObserver = TestObserver<com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem>()
        parser.parseVectorFile(validFile, project).subscribe(testObserver)
        
        // Verify
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        
        // Verify both colors were resolved
        verify(mockColorResolver).resolveColorReference("@color/primary", project)
        verify(mockColorResolver).resolveColorReference("@color/secondary", project)
        
        // Clean up
        tempFile.delete()
    }
    
    @Test
    fun testParseVectorWithoutColorReferences() {
        // Create a vector without color references
        val vectorContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24"
                android:viewportHeight="24">
                <path
                    android:fillColor="#FF0000"
                    android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z"/>
            </vector>
        """.trimIndent()
        
        val tempFile = createTempFile("test_vector_direct", ".xml")
        tempFile.writeText(vectorContent)
        
        // Create ValidFile
        val validFile = ValidFile(tempFile, tempFile.parent)
        
        // Parse vector
        val testObserver = TestObserver<com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem>()
        parser.parseVectorFile(validFile, project).subscribe(testObserver)
        
        // Verify
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        
        // Verify color resolver was not called for direct hex colors
        verify(mockColorResolver, never()).resolveColorReference(any(), any())
        
        // Clean up
        tempFile.delete()
    }
    
    @Test
    fun testParseVectorWithUnresolvedColorFallsBackToBlack() {
        // Create a vector with unresolved color reference
        val vectorContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24"
                android:viewportHeight="24">
                <path
                    android:fillColor="@color/unknown"
                    android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z"/>
            </vector>
        """.trimIndent()
        
        val tempFile = createTempFile("test_vector_unknown", ".xml")
        tempFile.writeText(vectorContent)
        
        // Mock color resolution returning null
        whenever(mockColorResolver.resolveColorReference("@color/unknown", project))
            .thenReturn(null)
        
        // Create ValidFile
        val validFile = ValidFile(tempFile, tempFile.parent)
        
        // Parse vector
        val testObserver = TestObserver<com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem>()
        parser.parseVectorFile(validFile, project).subscribe(testObserver)
        
        // Verify - should still parse successfully with fallback color
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        
        // Verify color resolver was called
        verify(mockColorResolver).resolveColorReference("@color/unknown", project)
        
        // Clean up
        tempFile.delete()
    }
    
    @Test
    fun testParseInvalidVectorFile() {
        // Create an invalid vector file
        val invalidContent = "This is not XML"
        
        val tempFile = createTempFile("test_vector_invalid", ".xml")
        tempFile.writeText(invalidContent)
        
        // Create ValidFile
        val validFile = ValidFile(tempFile, tempFile.parent)
        
        // Parse vector
        val testObserver = TestObserver<com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem>()
        parser.parseVectorFile(validFile, project).subscribe(testObserver)
        
        // Verify - should complete without emitting any items
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        
        // Clean up
        tempFile.delete()
    }
}