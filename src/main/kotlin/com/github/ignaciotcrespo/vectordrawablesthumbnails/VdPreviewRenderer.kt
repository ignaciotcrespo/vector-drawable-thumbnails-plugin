package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.android.ide.common.vectordrawable.VdPreview
import java.awt.image.BufferedImage

class VdPreviewRenderer : VectorImageRenderer {

    override fun render(attributes: ParsedVectorAttributes): BufferedImage? {
        return try {
            val log = StringBuilder() // VdPreview.getPreviewFromVectorDocument might use this
            VdPreview.getPreviewFromVectorDocument(
                VdPreview.TargetSize.createSizeFromWidth(50), // Default size, can be made configurable
                attributes.document,
                log
            )
        } catch (t: Throwable) {
            // Log error or handle appropriately
            println("Error rendering vector ${attributes.name}: ${t.message}")
            null
        }
    }
}
