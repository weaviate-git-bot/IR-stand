package ru.itmo.stand.util

import kotlin.math.exp

infix fun FloatArray.dot(other: FloatArray): Float {
    var out = 0.0F
    for (i in indices) out += this[i] * other[i]
    return out
}

fun softmax(numbers: FloatArray): FloatArray {
    val sum = numbers.map { exp(it) }.sum()
    return numbers.map { exp(it) / sum }.toFloatArray()
}
