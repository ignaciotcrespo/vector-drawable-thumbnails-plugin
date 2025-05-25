package com.github.ignaciotcrespo.vectordrawablesthumbnails

interface VectorAttributeParser {
    fun parse(xmlContent: String, fileName: String, fileSize: Long, validFile: ValidFile): ParsedVectorAttributes?
}
