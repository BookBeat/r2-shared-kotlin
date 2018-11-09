package org.readium.r2.shared

import kotlin.math.min

data class PageProgress(val documentStartProgress: Double,
                        val documentEndProgress: Double,
                        val totalStartProgress: Double,
                        val totalEndProgress: Double)

data class PublicationPosition(val documentIndex: Int,
                               val progressInDocument: Double)

data class SpineContentLength(val spineItem: Link,
                              val contentLength: Int,
                              val percentOfTotal: Double)

class ContentLengthInfo(contentLengthPairs: List<Pair<Link, Int>>) {

    var totalLength: Int = 0
    var spineContentLengths: List<SpineContentLength> = emptyList()

    init {
        contentLengthPairs.forEach { totalLength += it.second }

        if (totalLength != 0) {
            spineContentLengths = contentLengthPairs.map { pair ->
                val percent = pair.second.toDouble() / totalLength.toDouble()
                SpineContentLength(pair.first, pair.second, percent)
            }
        }
    }

    fun pageProgressFor(currentDocumentIndex: Int, currentPageInDocument: Int, documentTotalPages: Int): PageProgress {
        if (currentPageInDocument < 1 || currentPageInDocument > documentTotalPages) throw Throwable("Invalid pages")

        val documentStartProgress = (currentPageInDocument - 1).toDouble() / documentTotalPages.toDouble()
        val documentEndProgress = currentPageInDocument.toDouble() / documentTotalPages.toDouble()

        return pageProgressFor(currentDocumentIndex,
                documentStartProgress,
                documentEndProgress)
    }

    fun pageProgressFor(currentDocumentIndex: Int, progressInDocument: Double): PageProgress {
        return pageProgressFor(currentDocumentIndex,
                progressInDocument,
                progressInDocument)
    }

    private fun pageProgressFor(currentDocumentIndex: Int, documentStartProgress: Double, documentEndProgress: Double): PageProgress {
        var startOfDocumentTotalProgress = 0.0

        for (i in 0 until spineContentLengths.size) {
            if (i >= currentDocumentIndex) break
            startOfDocumentTotalProgress += spineContentLengths[i].percentOfTotal
        }

        val documentLengthPercentOfTotal = spineContentLengths[currentDocumentIndex].percentOfTotal

        val pageStartPercentOfTotal = documentLengthPercentOfTotal * documentStartProgress
        val pageStartTotalProgress = startOfDocumentTotalProgress + pageStartPercentOfTotal

        val pageEndPercentOfTotal = documentLengthPercentOfTotal * documentEndProgress
        val pageEndTotalProgress = min(startOfDocumentTotalProgress + pageEndPercentOfTotal, 1.0)

        return PageProgress(documentStartProgress,
                documentEndProgress,
                pageStartTotalProgress,
                pageEndTotalProgress)
    }

    fun positionFor(totalStartProgress: Double): PublicationPosition? {
        // this can happen sometimes - solve it by returning null and handle the position where it's used
        if (spineContentLengths.isEmpty()) return null
        var theSpineInfo = spineContentLengths.first()
        var documentIndex = 0
        var startOfDocumentTotalProgress = 0.0

        while (totalStartProgress >= startOfDocumentTotalProgress + theSpineInfo.percentOfTotal) {
            val previousSpineInfo = theSpineInfo
            if (documentIndex == spineContentLengths.size - 1) break
            documentIndex++
            theSpineInfo = spineContentLengths[documentIndex]
            startOfDocumentTotalProgress += previousSpineInfo.percentOfTotal
        }

        val totalPercentIntoDocument = totalStartProgress - startOfDocumentTotalProgress
        val progressInDocument = min(totalPercentIntoDocument / theSpineInfo.percentOfTotal, 1.0)

        return PublicationPosition(documentIndex, progressInDocument)
    }
}
