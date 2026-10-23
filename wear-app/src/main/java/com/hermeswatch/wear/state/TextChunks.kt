package com.hermeswatch.wear.state

/**
 * Split a long answer into short, readable items for ScalingLazyColumn.
 *
 * ScalingLazyColumn centres and scales whole items; one giant Text item would
 * start off-screen and scale awkwardly. Splitting on lines, then on sentence
 * ends (。！？.!? and friends) keeps every item ≤ [maxChars] so the crown scrolls
 * paragraph-by-paragraph with the fisheye effect intact.
 */
object TextChunks {
    private val SENTENCE_END = Regex("(?<=[。！？!?；;])|(?<=\\.)(?=\\s)")

    fun split(text: String, maxChars: Int = 140): List<String> {
        val out = mutableListOf<String>()
        for (rawLine in text.split('\n')) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.length <= maxChars) {
                out += line
                continue
            }
            val buf = StringBuilder()
            for (sentence in line.split(SENTENCE_END).map { it.trim() }.filter { it.isNotEmpty() }) {
                if (buf.isNotEmpty() && buf.length + 1 + sentence.length > maxChars) {
                    out += buf.toString()
                    buf.clear()
                }
                if (sentence.length > maxChars) {
                    if (buf.isNotEmpty()) { out += buf.toString(); buf.clear() }
                    out += hardWrap(sentence, maxChars)
                    continue
                }
                if (buf.isNotEmpty() && needsSpace(buf.last(), sentence.first())) buf.append(' ')
                buf.append(sentence)
            }
            if (buf.isNotEmpty()) out += buf.toString()
        }
        return out
    }

    private fun needsSpace(prev: Char, next: Char): Boolean = !isCjk(prev) && !isCjk(next)

    private fun isCjk(c: Char): Boolean {
        val b = Character.UnicodeBlock.of(c)
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            b == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
            b == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ||
            b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
    }

    private fun hardWrap(s: String, max: Int): List<String> {
        val parts = mutableListOf<String>()
        var rest = s
        while (rest.length > max) {
            val cut = rest.lastIndexOf(' ', max).takeIf { it > max / 2 } ?: max
            parts += rest.substring(0, cut).trim()
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) parts += rest
        return parts
    }
}
