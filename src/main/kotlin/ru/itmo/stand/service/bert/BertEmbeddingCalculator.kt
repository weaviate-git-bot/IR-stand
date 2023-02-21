package ru.itmo.stand.service.bert

import org.springframework.stereotype.Service

@Service
class BertEmbeddingCalculator(private val bertModelLoader: BertModelLoader) {

    private val predictor by lazy { bertModelLoader.tinyModel().newPredictor() }

    fun calculate(content: String): FloatArray = predictor.predict(arrayOf(content)).first()

    fun calculate(contents: Array<String>): Array<FloatArray> = predictor.predict(contents)
}
