package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.File
import kotlin.test.assertEquals

/**
 * Test class demonstrating the testability of the refactored architecture.
 */
class DefaultVectorFilterTest {

    private val filter = DefaultVectorFilter()

    @Test
    fun `should return all items when filter text is null`() {
        // Arrange
        val mockImage = BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB)
        val mockFile = File("test.xml")
        val validFile = ValidFile(mockFile, "/test")
        
        val items = listOf(
            VectorItem("vector1.xml", mockImage, validFile, 24, 24, 1024),
            VectorItem("vector2.xml", mockImage, validFile, 48, 48, 2048)
        )

        // Act
        val result = filter.filter(items, null)

        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun `should filter items by name containing filter text`() {
        // Arrange
        val mockImage = BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB)
        val mockFile = File("test.xml")
        val validFile = ValidFile(mockFile, "/test")
        
        val items = listOf(
            VectorItem("icon_home.xml", mockImage, validFile, 24, 24, 1024),
            VectorItem("icon_settings.xml", mockImage, validFile, 48, 48, 2048),
            VectorItem("button_save.xml", mockImage, validFile, 32, 32, 1536)
        )

        // Act
        val result = filter.filter(items, "icon")

        // Assert
        assertEquals(2, result.size)
        assertEquals("icon_home.xml", result[0].name)
        assertEquals("icon_settings.xml", result[1].name)
    }
} 