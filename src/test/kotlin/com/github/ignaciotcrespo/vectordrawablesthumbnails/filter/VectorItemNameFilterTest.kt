package com.github.ignaciotcrespo.vectordrawablesthumbnails.filter

import com.github.ignaciotcrespo.vectordrawablesthumbnails.VectorItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.awt.image.BufferedImage

class VectorItemNameFilterTest {

    private lateinit var filter: VectorItemNameFilter
    private lateinit var mockImage: BufferedImage // Mock BufferedImage as it's part of VectorItem

    @BeforeEach
    fun setUp() {
        filter = VectorItemNameFilter()
        mockImage = mock() // Initialize the mocked BufferedImage
    }

    private fun createVectorItem(name: String): VectorItem {
        // Create a basic VectorItem for testing. Details like image, validFile, etc.,
        // are not relevant for name filtering, so they can be minimal or mocked.
        return VectorItem(name, mockImage, mock(), 0, 0, 0L)
    }

    @Test
    fun `filter with empty list`() {
        val items = emptyList<VectorItem>()
        val result = filter.filter(items, "test")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filter with null query`() {
        val items = listOf(createVectorItem("apple"), createVectorItem("banana"))
        val result = filter.filter(items, null)
        assertEquals(2, result.size)
        assertEquals(items, result) // Should return all items
    }

    @Test
    fun `filter with empty query`() {
        val items = listOf(createVectorItem("apple"), createVectorItem("banana"))
        val result = filter.filter(items, "")
        assertEquals(2, result.size)
        assertEquals(items, result) // Should return all items
    }

    @Test
    fun `filter with matching query (case-insensitive)`() {
        val items = listOf(
            createVectorItem("ic_apple_pie"),
            createVectorItem("ic_banana_split"),
            createVectorItem("ic_orange_juice")
        )
        val result = filter.filter(items, "apple")
        assertEquals(1, result.size)
        assertEquals("ic_apple_pie", result[0].name)

        val resultCaps = filter.filter(items, "ORANGE")
        assertEquals(1, resultCaps.size)
        assertEquals("ic_orange_juice", resultCaps[0].name)
    }

    @Test
    fun `filter with query matching multiple items`() {
        val items = listOf(
            createVectorItem("ic_apple_pie"),
            createVectorItem("ic_apple_cider"),
            createVectorItem("ic_banana_split")
        )
        val result = filter.filter(items, "apple")
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "ic_apple_pie" })
        assertTrue(result.any { it.name == "ic_apple_cider" })
    }

    @Test
    fun `filter with query not matching any items`() {
        val items = listOf(
            createVectorItem("ic_apple_pie"),
            createVectorItem("ic_banana_split")
        )
        val result = filter.filter(items, "grape")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filter with query matching part of a name`() {
        val items = listOf(createVectorItem("vector_one_icon"), createVectorItem("icon_two_vector"))
        val result = filter.filter(items, "vector")
        assertEquals(2, result.size)
    }
}
