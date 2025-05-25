package com.github.ignaciotcrespo.vectordrawablesthumbnails

import org.w3c.dom.Document

data class ParsedVectorAttributes(
    val name: String,
    val viewportW: Int,
    val viewportH: Int,
    val fileSize: Long,
    val document: Document,
    val validFile: ValidFile
)
