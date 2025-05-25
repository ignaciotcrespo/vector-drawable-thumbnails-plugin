package com.github.ignaciotcrespo.vectordrawablesthumbnails

import java.awt.image.BufferedImage

interface VectorImageRenderer {
    fun render(attributes: ParsedVectorAttributes): BufferedImage?
}
