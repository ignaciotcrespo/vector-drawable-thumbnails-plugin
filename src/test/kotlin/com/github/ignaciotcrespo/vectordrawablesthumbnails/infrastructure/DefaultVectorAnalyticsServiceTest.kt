package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultVectorAnalyticsServiceTest {
    
    private val analyticsService = DefaultVectorAnalyticsService()
    
    @Test
    fun `should analyze vector and return analytics`() {
        // Create a test vector XML content
        val xmlContent = """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24"
                android:viewportHeight="24">
                <path android:fillColor="#FF000000" android:pathData="M12,2l3.09,6.26L22,9.27l-5,4.87L18.18,22L12,18.77L5.82,22L7,14.14L2,9.27l6.91-1.01L12,2z"/>
            </vector>
        """.trimIndent()
        
        // Create a temporary file
        val tempFile = File.createTempFile("test_vector", ".xml")
        tempFile.writeText(xmlContent)
        tempFile.deleteOnExit()
        
        // Create a test VectorItem
        val vectorItem = VectorItem(
            name = "ic_star.xml",
            image = BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB),
            validFile = ValidFile(tempFile, System.getProperty("java.io.tmpdir")),
            viewportW = 24,
            viewportH = 24,
            fileSize = xmlContent.length.toLong()
        )
        
        // Analyze the vector
        val analytics = analyticsService.analyzeVector(vectorItem)
        
        // Verify analytics
        assertNotNull(analytics)
        assertEquals(ComplexityLevel.SIMPLE, analytics.complexityLevel)
        assertEquals(1, analytics.pathCount)
        assertTrue(analytics.complexityScore > 0)
        assertTrue(analytics.estimatedRenderTime > 0)
        assertTrue(analytics.tags.isNotEmpty())
        assertEquals(1.0, analytics.aspectRatio, 0.01)
    }
    
    @Test
    fun `should extract tags from filename`() {
        val tempFile = File.createTempFile("ic_home", ".xml")
        tempFile.writeText("<vector></vector>")
        tempFile.deleteOnExit()
        
        val vectorItem = VectorItem(
            name = "ic_home.xml",
            image = BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB),
            validFile = ValidFile(tempFile, System.getProperty("java.io.tmpdir")),
            viewportW = 24,
            viewportH = 24,
            fileSize = 100
        )
        
        val tags = analyticsService.extractTags(vectorItem)
        
        assertTrue(tags.contains("icon"))
        assertTrue(tags.contains("navigation"))
        assertTrue(tags.contains("square"))
    }
} 