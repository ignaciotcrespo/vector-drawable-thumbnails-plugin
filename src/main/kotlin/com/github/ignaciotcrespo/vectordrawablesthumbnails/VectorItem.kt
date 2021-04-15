package com.github.ignaciotcrespo.vectordrawablesthumbnails

import java.awt.image.BufferedImage

class VectorItem(
    var name: String,
    var image: BufferedImage,
    var validFile: ValidFile,
    val viewportW: Int = 0,
    val viewportH: Int = 0,
    val fileSize: Long = 0
)