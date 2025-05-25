package com.github.ignaciotcrespo.vectordrawablesthumbnails.sorter

import com.github.ignaciotcrespo.vectordrawablesthumbnails.VectorItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.awt.image.BufferedImage

class VectorItemPropertySorterTest {

    private lateinit var sorter: VectorItemPropertySorter
    private lateinit var mockImage: BufferedImage // Mock BufferedImage for VectorItem

    @BeforeEach
    fun setUp() {
        sorter = VectorItemPropertySorter()
        mockImage = mock() // Initialize the mocked BufferedImage
    }

    private fun createVectorItem(name: String, width: Int, height: Int, fileSize: Long): VectorItem {
        return VectorItem(name, mockImage, mock(), width, height, fileSize)
    }

    private val unsortedItems = listOf(
        createVectorItem("charlie_icon", 30, 30, 300L), // W*H = 900
        createVectorItem("alpha_vector", 10, 50, 100L), // W*H = 500
        createVectorItem("beta_image", 20, 20, 200L)    // W*H = 400
    )

    private val itemsWithIdenticalNames = listOf(
        createVectorItem("icon", 30, 30, 300L),
        createVectorItem("icon", 10, 50, 100L),
        createVectorItem("icon", 20, 20, 200L)
    )

    @Test
    fun `sort with empty list`() {
        val items = emptyList<VectorItem>()
        val result = sorter.sort(items, "By Name", "Asc")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `sort by Name Ascending`() {
        val result = sorter.sort(unsortedItems, "By Name", "Asc")
        assertEquals("alpha_vector", result[0].name)
        assertEquals("beta_image", result[1].name)
        assertEquals("charlie_icon", result[2].name)
    }

    @Test
    fun `sort by Name Descending`() {
        val result = sorter.sort(unsortedItems, "By Name", "Desc")
        assertEquals("charlie_icon", result[0].name)
        assertEquals("beta_image", result[1].name)
        assertEquals("alpha_vector", result[2].name)
    }

    @Test
    fun `sort by Width Ascending`() {
        val result = sorter.sort(unsortedItems, "By Width", "Asc")
        assertEquals(10, result[0].viewportW) // alpha_vector
        assertEquals(20, result[1].viewportW) // beta_image
        assertEquals(30, result[2].viewportW) // charlie_icon
    }

    @Test
    fun `sort by Width Descending`() {
        val result = sorter.sort(unsortedItems, "By Width", "Desc")
        assertEquals(30, result[0].viewportW) // charlie_icon
        assertEquals(20, result[1].viewportW) // beta_image
        assertEquals(10, result[2].viewportW) // alpha_vector
    }

    @Test
    fun `sort by Height Ascending`() {
        val result = sorter.sort(unsortedItems, "By Height", "Asc")
        assertEquals(20, result[0].viewportH) // beta_image
        assertEquals(30, result[1].viewportH) // charlie_icon
        assertEquals(50, result[2].viewportH) // alpha_vector
    }

    @Test
    fun `sort by Height Descending`() {
        val result = sorter.sort(unsortedItems, "By Height", "Desc")
        assertEquals(50, result[0].viewportH) // alpha_vector
        assertEquals(30, result[1].viewportH) // charlie_icon
        assertEquals(20, result[2].viewportH) // beta_image
    }

    @Test
    fun `sort by Width x Height Ascending`() {
        val result = sorter.sort(unsortedItems, "By Width x Height", "Asc")
        assertEquals(400, result[0].viewportW * result[0].viewportH) // beta_image
        assertEquals(500, result[1].viewportW * result[1].viewportH) // alpha_vector
        assertEquals(900, result[2].viewportW * result[2].viewportH) // charlie_icon
    }

    @Test
    fun `sort by Width x Height Descending`() {
        val result = sorter.sort(unsortedItems, "By Width x Height", "Desc")
        assertEquals(900, result[0].viewportW * result[0].viewportH) // charlie_icon
        assertEquals(500, result[1].viewportW * result[1].viewportH) // alpha_vector
        assertEquals(400, result[2].viewportW * result[2].viewportH) // beta_image
    }

    @Test
    fun `sort by File Size Ascending`() {
        val result = sorter.sort(unsortedItems, "By File Size", "Asc")
        assertEquals(100L, result[0].fileSize) // alpha_vector
        assertEquals(200L, result[1].fileSize) // beta_image
        assertEquals(300L, result[2].fileSize) // charlie_icon
    }

    @Test
    fun `sort by File Size Descending`() {
        val result = sorter.sort(unsortedItems, "By File Size", "Desc")
        assertEquals(300L, result[0].fileSize) // charlie_icon
        assertEquals(200L, result[1].fileSize) // beta_image
        assertEquals(100L, result[2].fileSize) // alpha_vector
    }

    @Test
    fun `sort with null sortProperty (defaults to Ascending Name or no change)`() {
        // Current implementation defaults to no specific order if property is unknown,
        // but if it were to default to Name Asc:
        val result = sorter.sort(unsortedItems, null, "Asc")
        // This test depends on the default behavior. If it's "no change", then:
        // assertEquals(unsortedItems, result)
        // If it defaults to "By Name" Asc:
        assertEquals("alpha_vector", result[0].name)
        assertEquals("beta_image", result[1].name)
        assertEquals("charlie_icon", result[2].name)
    }

    @Test
    fun `sort with null direction (defaults to Ascending)`() {
        val result = sorter.sort(unsortedItems, "By Name", null)
        assertEquals("alpha_vector", result[0].name)
        assertEquals("beta_image", result[1].name)
        assertEquals("charlie_icon", result[2].name)
    }

    @Test
    fun `sort with items having identical values for a sort property (Name)`() {
        // Sorting by Width Asc when names are identical
        val result = sorter.sort(itemsWithIdenticalNames, "By Width", "Asc")
        assertEquals(10, result[0].viewportW) // icon (10x50)
        assertEquals(20, result[1].viewportW) // icon (20x20)
        assertEquals(30, result[2].viewportW) // icon (30x30)
        // All names are "icon", so original relative order for identical widths might be preserved
        // or determined by subsequent stable sort criteria (not explicitly defined here).
        // The primary assertion is that widths are sorted.
    }

    @Test
    fun `sort with unknown sortProperty`() {
        val result = sorter.sort(unsortedItems, "By Unknown Property", "Asc")
        // Expect the list to be returned as is, or sorted by a default if implemented.
        // Based on current implementation, it will be sorted by Name Asc as a fallback.
        assertEquals("alpha_vector", result[0].name)
        assertEquals("beta_image", result[1].name)
        assertEquals("charlie_icon", result[2].name)
    }
}
