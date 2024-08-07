package me.rosuh.sieve.utils

import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <A> catchIO(
    block: suspend () -> A
): Either<Throwable, A> = Either.catch {
    withContext(Dispatchers.IO) {
        block()
    }
}