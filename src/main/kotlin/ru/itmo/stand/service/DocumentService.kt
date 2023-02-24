package ru.itmo.stand.service

import ru.itmo.stand.config.Method
import ru.itmo.stand.service.model.Format
import java.io.File

interface DocumentService {

    val method: Method

    fun find(id: String): String?
    fun search(queries: File, format: Format): List<String>
    fun save(content: String, withId: Boolean): String
    fun saveInBatch(contents: Sequence<String>, withId: Boolean): List<String>
    fun getFootprint(): String
}
