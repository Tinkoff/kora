package ru.tinkoff.kora.common

import kotlin.coroutines.CoroutineContext

fun CoroutineContext.currentKoraContext(): Context = Context.Kotlin.current(this)

fun CoroutineContext.inject(context: Context): CoroutineContext = Context.Kotlin.inject(this, context)
