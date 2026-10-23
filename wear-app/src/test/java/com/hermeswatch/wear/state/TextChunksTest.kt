package com.hermeswatch.wear.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextChunksTest {
    @Test
    fun shortTextIsOneChunk() {
        assertEquals(listOf("晴，28°C"), TextChunks.split("晴，28°C"))
    }

    @Test
    fun splitsOnLinesAndDropsBlanks() {
        assertEquals(listOf("a", "b"), TextChunks.split("a\n\n  \nb\n"))
    }

    @Test
    fun longCantoneseSplitsOnSentenceEnds() {
        val s = "今日有三件事要做。".repeat(10) // 90 chars
        val chunks = TextChunks.split(s, maxChars = 40)
        assertTrue(chunks.all { it.length <= 40 })
        assertEquals(s, chunks.joinToString(""))
    }

    @Test
    fun longEnglishKeepsSpacesBetweenSentences() {
        val s = "This is sentence one. This is sentence two. This is sentence three."
        val chunks = TextChunks.split(s, maxChars = 45)
        assertTrue(chunks.all { it.length <= 45 })
        assertEquals(s, chunks.joinToString(" "))
    }

    @Test
    fun hardWrapsAnUnbrokenRun() {
        val s = "x".repeat(300)
        val chunks = TextChunks.split(s, maxChars = 100)
        assertEquals(3, chunks.size)
        assertEquals(s, chunks.joinToString(""))
    }

    @Test
    fun emptyGivesNoChunks() {
        assertTrue(TextChunks.split("  \n ").isEmpty())
    }
}
