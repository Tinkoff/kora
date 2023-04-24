package ru.tinkoff.kora.database.vertx.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
interface VertxRepository {
    val vertxConnectionFactory: VertxConnectionFactory
}
