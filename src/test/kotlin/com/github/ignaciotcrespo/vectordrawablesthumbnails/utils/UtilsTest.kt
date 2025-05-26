package com.github.ignaciotcrespo.vectordrawablesthumbnails.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.io.ByteArrayInputStream
import java.io.IOException

@ExtendWith(MockitoExtension::class)
class UtilsTest {

    @Test
    fun `read should convert InputStream to String correctly`() {
        // Given
        val testContent = "Hello\nWorld\nTest"
        val inputStream = ByteArrayInputStream(testContent.toByteArray())

        // When
        val result = Utils.read(inputStream)

        // Then
        assert(result == testContent) { "Should read content correctly, got: $result" }
    }

    @Test
    fun `read should handle empty InputStream`() {
        // Given
        val inputStream = ByteArrayInputStream(byteArrayOf())

        // When
        val result = Utils.read(inputStream)

        // Then
        assert(result.isEmpty()) { "Should return empty string for empty input" }
    }

    @Test
    fun `read should handle single line content`() {
        // Given
        val testContent = "Single line content"
        val inputStream = ByteArrayInputStream(testContent.toByteArray())

        // When
        val result = Utils.read(inputStream)

        // Then
        assert(result == testContent) { "Should read single line correctly" }
    }

    @Test
    fun `read should handle multiline content with different line endings`() {
        // Given
        val testContent = "Line 1\nLine 2\nLine 3"
        val inputStream = ByteArrayInputStream(testContent.toByteArray())

        // When
        val result = Utils.read(inputStream)

        // Then
        assert(result == testContent) { "Should preserve line endings" }
        assert(result.contains("\n")) { "Should contain newline characters" }
    }

    @Test
    fun `read should handle null InputStream gracefully`() {
        // When/Then
        try {
            Utils.read(null)
            assert(false) { "Should throw exception for null input" }
        } catch (e: Exception) {
            // Expected behavior - null input should cause an exception
            assert(e is IOException || e is NullPointerException) { "Should throw appropriate exception for null input" }
        }
    }

    @Test
    fun `read should handle content with special characters`() {
        // Given
        val testContent = "Special chars: àáâãäåæçèéêë 中文 🎉"
        val inputStream = ByteArrayInputStream(testContent.toByteArray(Charsets.UTF_8))

        // When
        val result = Utils.read(inputStream)

        // Then
        assert(result == testContent) { "Should handle special characters correctly" }
    }
} 