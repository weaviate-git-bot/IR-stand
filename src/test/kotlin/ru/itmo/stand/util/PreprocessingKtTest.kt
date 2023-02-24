package ru.itmo.stand.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PreprocessingKtTest {

    @Nested
    inner class CreateWindows {
        private val tokens = (
            "Some sentence that is used in tests " +
                "to check the correctness of the function"
            ).split(" ")

        @Test
        fun `should throw when even size is passed`() {
            val actualEx = assertThrows(IllegalArgumentException::class.java) {
                tokens.createWindows(2)
            }
            assertEquals("Size value should be odd", actualEx.message)
        }

        @Test
        fun `should throw when negative size is passed`() {
            val actualEx = assertThrows(IllegalArgumentException::class.java) {
                tokens.createWindows(-1)
            }
            assertEquals("Size value should be greater than zero", actualEx.message)
        }

        @Test
        fun `should return one window when list size is less or equal to partial window size`() {
            val actualWhenListSizeLessThanPartialWindowSize = tokens.createWindows(29)
            val actualWhenListSizeEqualToPartialWindowSize = tokens.createWindows(27)
            assertEquals(Window(content = tokens), actualWhenListSizeLessThanPartialWindowSize[0])
            assertEquals(Window(content = tokens), actualWhenListSizeEqualToPartialWindowSize[0])
        }

        // @Test FIXME
        // fun `should return only partial windows`() {
        /* Try something like that:
        1. (0 until sideTokensCount)
            .takeWhile { index -> (partialWindowSize + index) <= this.size }
            .forEach { index -> result.add(this.subList(0, partialWindowSize + index)) }
        2. (this.size - size + 1 until this.size - sideTokensCount)
            .filter { it >=0 }
            .forEach { index -> result.add(this.subList(index, this.size)) }
         */
        // val windows = tokens.createWindows(25)
        // println(windows)
        // }

        @Test
        fun `should returns partial and full windows`() {
            val windows = tokens.createWindows(5)
            assertEquals(
                listOf(
                    Window("Some", listOf("Some", "sentence", "that")),
                    Window("sentence", listOf("Some", "sentence", "that", "is")),
                    Window("that", listOf("Some", "sentence", "that", "is", "used")),
                    Window("is", listOf("sentence", "that", "is", "used", "in")),
                    Window("used", listOf("that", "is", "used", "in", "tests")),
                    Window("in", listOf("is", "used", "in", "tests", "to")),
                    Window("tests", listOf("used", "in", "tests", "to", "check")),
                    Window("to", listOf("in", "tests", "to", "check", "the")),
                    Window("check", listOf("tests", "to", "check", "the", "correctness")),
                    Window("the", listOf("to", "check", "the", "correctness", "of")),
                    Window("correctness", listOf("check", "the", "correctness", "of", "the")),
                    Window("of", listOf("the", "correctness", "of", "the", "function")),
                    Window("the", listOf("correctness", "of", "the", "function")),
                    Window("function", listOf("of", "the", "function")),
                ),
                windows,
            )
        }
    }
}