package id.teratai.dompet.ml

import id.teratai.dompet.parse.ReceiptHeuristicParser

object TotalLineSelector {

    data class Result(
        val bestLine: String?,
        val bestScore: Float,
        val totalNorm: String?,
    )

    fun pickTotalFromLines(model: TotalLineModel, lines: List<String>): Result {
        var bestLine: String? = null
        var bestScore = -1f

        for (ln in lines) {
            val s = model.score(ln)
            if (s > bestScore) {
                bestScore = s
                bestLine = ln
            }
        }

        val total = bestLine?.let { ReceiptHeuristicParser.extractBestMoneyFromLine(it) }
        return Result(bestLine = bestLine, bestScore = bestScore, totalNorm = total)
    }
}
