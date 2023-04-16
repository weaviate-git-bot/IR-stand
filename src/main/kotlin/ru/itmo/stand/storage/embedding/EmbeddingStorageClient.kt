package ru.itmo.stand.storage.embedding

import io.weaviate.client.WeaviateClient
import io.weaviate.client.base.Result
import io.weaviate.client.v1.batch.model.ObjectGetResponse
import io.weaviate.client.v1.data.model.WeaviateObject
import io.weaviate.client.v1.data.replication.model.ConsistencyLevel
import io.weaviate.client.v1.graphql.query.argument.NearVectorArgument
import io.weaviate.client.v1.graphql.query.fields.Field
import io.weaviate.client.v1.misc.model.VectorIndexConfig
import io.weaviate.client.v1.schema.model.Property
import io.weaviate.client.v1.schema.model.Schema
import io.weaviate.client.v1.schema.model.WeaviateClass
import org.springframework.stereotype.Service
import ru.itmo.stand.storage.embedding.model.ContextualizedEmbedding

@Service
class EmbeddingStorageClient(
    private val client: WeaviateClient,
) {

    private val className = checkNotNull(ContextualizedEmbedding::class.simpleName)
    private val tokenField = Field.builder().name(ContextualizedEmbedding::token.name).build()
    private val docIdField = Field.builder().name(ContextualizedEmbedding::embeddingId.name).build()
    private val additionalField = Field.builder()
        .name("_additional")
        .fields(
            Field.builder().name("vector").build(),
            Field.builder().name("distance").build(),
            Field.builder().name("certainty").build(),
        )
        .build()

    fun findByVector(vector: Array<Float>): List<ContextualizedEmbedding> {
        val result = client.graphQL()
            .get()
            .withClassName(className)
            .withFields(tokenField, docIdField, additionalField)
            .withNearVector(NearVectorArgument.builder().vector(vector).certainty(0.95f).build()) // TODO: configure this value
            .withLimit(10) // TODO: configure this value
            .run()
        check(result.error == null) { "Weaviate error: ${result.error}" }
        check(result.result.errors == null) { "GraphQL errors: ${result.result.errors}" }
        @Suppress("UNCHECKED_CAST")
        return checkNotNull((result.result.data as Map<String, Map<String, List<Map<String, *>>>>)["Get"]?.get("ContextualizedEmbedding"))
            .map { obj ->
                val additional = obj["_additional"] as Map<String, List<Double>>
                ContextualizedEmbedding(
                    token = obj["token"] as String,
                    embeddingId = (obj["embeddingId"] as Double).toInt(),
                    embedding = checkNotNull(additional["vector"]?.map { it.toFloat() }?.toTypedArray()),
                )
            }
    }

    fun findSchema(): Result<Schema> = client.schema()
        .getter()
        .run()

    fun deleteAllModels(): Result<Boolean> = client.schema()
        .allDeleter()
        .run()

    fun index(embedding: ContextualizedEmbedding): Result<Array<ObjectGetResponse>> {
        val obj = WeaviateObject.builder()
            .vector(embedding.embedding)
            .properties(
                mapOf(
                    ContextualizedEmbedding::token.name to embedding.token,
                    ContextualizedEmbedding::embeddingId.name to embedding.embeddingId,
                ),
            )
            .className(className)
            .build()

        return client.batch().objectsBatcher()
            .withObjects(obj)
            .withConsistencyLevel(ConsistencyLevel.ONE)
            .run()
    }

    fun indexBatch(embeddings: List<ContextualizedEmbedding>): Result<Array<ObjectGetResponse>> {
        val objects = embeddings.map {
            WeaviateObject.builder()
                .vector(it.embedding)
                .properties(
                    mapOf(
                        ContextualizedEmbedding::token.name to it.token,
                        ContextualizedEmbedding::embeddingId.name to it.embeddingId,
                    ),
                )
                .className(className)
                .build()
        }.toTypedArray()

        return client.batch().objectsBatcher()
            .withObjects(*objects)
            .withConsistencyLevel(ConsistencyLevel.ONE)
            .run()
    }

    fun ensureContextualizedEmbeddingModel(): Result<WeaviateClass> {
        val foundClass = client.schema().classGetter()
            .withClassName(className)
            .run()
        if (foundClass.result != null) return foundClass

        val weaviateClass = WeaviateClass.builder()
            .className(className)
            .vectorIndexType("hnsw")
            .vectorIndexConfig(VectorIndexConfig.builder().build())
            .properties(
                listOf(
                    Property.builder()
                        .name(ContextualizedEmbedding::embeddingId.name)
                        .dataType(listOf("int"))
                        .build(),
                    Property.builder()
                        .name(ContextualizedEmbedding::token.name)
                        .dataType(listOf("string"))
                        .build(),
                ),
            )
            .build()

        val createdClass = client.schema().classCreator()
            .withClass(weaviateClass)
            .run()

        if (createdClass.result) {
            return client.schema().classGetter()
                .withClassName(className)
                .run()
        }

        error("Failed to ensure class [$weaviateClass]. Error: ${createdClass.error}")
    }
}