package ru.itmo.stand.service.impl

import ai.djl.Application
import ai.djl.inference.Predictor
import ai.djl.repository.zoo.Criteria
import ai.djl.training.util.ProgressBar
import ai.djl.translate.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
import org.springframework.beans.factory.annotation.Autowired
import ru.itmo.stand.config.Method
import ru.itmo.stand.config.StandProperties
import ru.itmo.stand.service.DocumentService
import ru.itmo.stand.util.toNgrams
import java.io.BufferedReader
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

typealias KeyType = String
typealias ValueType = ConcurrentHashMap<String, Float>
typealias InvertedIndexType = MVMap<KeyType, ValueType>

abstract class BaseBertService(
    private val translator: Translator<String, FloatArray>,
) : DocumentService() {

    @Autowired
    protected lateinit var standProperties: StandProperties

    private val invertedIndexStore by lazy {
        val basePath = standProperties.app.basePath
        val invertedIndexPath = "$basePath/indexes/${method.name.lowercase()}/inverted_index.dat"
        File(invertedIndexPath).parentFile.mkdirs()
        MVStore.open(invertedIndexPath)
    }
    protected val predictor: Predictor<String, FloatArray> by lazy {
        val basePath = standProperties.app.basePath
        Criteria.builder()
            .optApplication(Application.NLP.TEXT_EMBEDDING)
            .setTypes(String::class.java, FloatArray::class.java)
            .optModelUrls("$basePath/models/${method.name.lowercase()}/distilbert.pt")
            .optTranslator(translator)
            .optProgress(ProgressBar())
            .build()
            .loadModel()
            .newPredictor(translator)
    }
    protected lateinit var invertedIndex: InvertedIndexType

    @PostConstruct
    private fun readInvertedIndex() {
        invertedIndex = runCatching { invertedIndexStore.openMap<KeyType, ValueType>("inverted_index") }
            .onFailure { log.error("Could not load inverted index", it) }
            .getOrThrow()
    }

    @PreDestroy
    private fun writeInvertedIndex() {
        invertedIndexStore.close()
    }

    abstract override val method: Method

    override fun find(id: String): String? {
        return invertedIndex.toString() // TODO: add impl for finding by id
    }

    fun HashForQuery(): HashMap<Int, String> {
        val bufferedReader: BufferedReader = File("collections/queries.air-subset.tsv").bufferedReader()
        val inputString = bufferedReader.use { it.readText() }
        val words = inputString.split("\r\n".toRegex())
        val words2 = words.toList()

        var map = HashMap<Int, String>()
        for (k in words2) {
            if (k == "") {
                break
            }
            val d = k.split("\t")
            map[d[0].toInt()] = d[1]
        }
        return map
    }


    override fun search(query: String): List<String> {
        var HashOfQueries = HashForQuery()
        val KeysList: MutableList<Int> = mutableListOf();
        val QueryList: MutableList<String> = mutableListOf();
        val ResultingList = mutableListOf<String>();
        var pressF: MutableList<String> = mutableListOf()
        for ((keyq, query1) in HashOfQueries) {
            if (keyq == 535142) { //по какой то причине падает на 1061472 id 535142
//                var a = 10
                break
            }
            QueryList.add(query1)
            KeysList.add(keyq)
            val tokens = preprocess(query1)
            val cdf = (tokens.asSequence()
                .mapNotNull { invertedIndex[it] }
                .reduce { m1, m2 ->
                    m2.forEach { (k, v) -> m1.merge(k, v) { v1, v2 -> v1.apply { v1 + v2 } } }
                    m1
                }
                .toList()
                .sortedByDescending { (_, score) -> score }
                .take(10)
                .map { (docId, _) -> docId })

            var counterZ = 0
            for (i in cdf) {
                pressF.add(keyq.toString().replace("[", "").replace("]", "") + "\t"+ "\t" + i.toString() + "\t" +counterZ)
                counterZ +=1
            }
          }

        File("collections/queriesForMRR.tsv").bufferedWriter().use { out ->

            for (i in pressF) {
            out.write(i)
            out.write("\n")
            }
        }

        val returningList = listOf<String>("finish")

        return returningList


//    override fun search(query: String): List<String> {
//        val tokens = preprocess(query)
//         return tokens.asSequence()
//             .mapNotNull { invertedIndex[it] }
//             .reduce { acc, scoreByDocIdMap ->
//                 scoreByDocIdMap.forEach { (docId, score) -> acc.merge(docId, score) { prev, new -> prev + new } }
//                 acc
//             }
//             .toList()
//             .sortedByDescending { (_, score) -> score }
//             .take(10)
//             .map { (docId, _) -> docId }
//     }

    /**
     * CLI command example: save -m CUSTOM "Around 9 Million people live in London"
     */

    abstract override fun save(content: String, withId: Boolean): String

    /**
     * CLI command example: save-in-batch -m CUSTOM --with-id data/collection.air-subset.tsv
     */
    override fun saveInBatch(contents: List<String>, withId: Boolean): List<String> = runBlocking(Dispatchers.Default) {
        log.info("Total size: ${contents.size}")
        val channel = Channel<String>(BATCH_SIZE_DOCUMENTS)
        contents.asSequence()
            .onEachIndexed { index, _ -> if (index % 10 == 0) log.info("Indexing is started for $index passages") }
            .forEach { content ->
                channel.send(content)
                launch { save(channel.receive(), withId) }
            }
        Collections.emptyList()
    }

    protected fun InvertedIndexType.index(token: String, score: Float, documentId: String) {
        this.merge(token, ConcurrentHashMap(mapOf(documentId to score))) { v1, v2 ->
            v1.apply { if (!containsKey(documentId)) putAll(v2) }
        }
    }

    protected fun preprocess(content: String): List<String> = preprocess(listOf(content))[0]

    protected open fun preprocess(contents: List<String>): List<List<String>> = contents
        .map { it.lowercase() }
        .map { it.toNgrams() }

    companion object {
        const val BATCH_SIZE_DOCUMENTS = 10
    }
}
