package ru.itmo.stand.service.impl.neighbours

import org.springframework.stereotype.Service
import ru.itmo.stand.config.StandProperties
import ru.itmo.stand.service.preprocessing.ContextSplitter
import ru.itmo.stand.service.preprocessing.StopWordRemover
import ru.itmo.stand.service.preprocessing.TextCleaner
import ru.itmo.stand.util.Window
import ru.itmo.stand.util.bertTokenizer

@Service
class PreprocessingPipelineExecutor(
    private val standProperties: StandProperties,
    private val contextSplitter: ContextSplitter,
    private val stopWordRemover: StopWordRemover,
    private val textCleaner: TextCleaner,
) {

    fun execute(content: String): List<Window> {
        val cleanedContent = textCleaner.preprocess(content)
        val tokens = bertTokenizer.tokenize(cleanedContent)
        val tokensWithoutStopWords = stopWordRemover.preprocess(tokens) // TODO: configure this value (try to delete it?)
        val windowSize = standProperties.app.neighboursAlgorithm.tokenBatchSize
        return contextSplitter.preprocess(ContextSplitter.Input(tokensWithoutStopWords, windowSize))
    }
}
