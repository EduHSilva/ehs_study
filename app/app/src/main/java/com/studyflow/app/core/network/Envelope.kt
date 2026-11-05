package com.studyflow.app.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Resposta padrão da API: tudo chega embrulhado em `DefaultResponse`. */
@Serializable
data class Envelope<T>(
    val message: String? = null,
    val data: T? = null,
    val links: Map<String, String>? = null,
)

/** `DELETE` também devolve o envelope, mas com `data` vazio. */
typealias EmptyEnvelope = Envelope<JsonElement?>

class ApiException(
    override val message: String,
    val status: Int? = null,
) : Exception(message)

fun <T> Envelope<T>.unwrap(): T = data ?: throw ApiException(message ?: "Resposta inesperada da API")
