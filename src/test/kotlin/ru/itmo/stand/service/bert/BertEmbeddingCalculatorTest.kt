package ru.itmo.stand.service.bert

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.itmo.stand.fixtures.standProperties

class BertEmbeddingCalculatorTest {

    private val embeddingCalculator = BertEmbeddingCalculator(
        BertModelLoader(DefaultBertTranslator(), standProperties()),
    )

    @Test
    fun `should return same result for batch mode`() {
        val content1 = "test 1"
        val content2 = "test 2 3"
        val content3 = "test 3 4 5"
        val vector1 = embeddingCalculator.calculate(content1)
        val vector2 = embeddingCalculator.calculate(content2)
        val vector3 = embeddingCalculator.calculate(content3)

        val embeddingsBatch = embeddingCalculator.calculate(arrayOf(content1, content2, content3))

        assertThat(embeddingsBatch).isEqualTo(arrayOf(vector1, vector2, vector3))
    }
}
