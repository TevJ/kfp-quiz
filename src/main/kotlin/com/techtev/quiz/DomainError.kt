package com.techtev.quiz

import arrow.core.NonEmptyList
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLens

sealed interface DomainError

data class IncorrectFields(val failures: NonEmptyList<FieldValidationFailure>) : DomainError {
    val message = "Issues with the following fields: ${failures.joinToString()}"
}

data class UserAlreadyExists(val email: Email) : DomainError {
    val message = "A user with email $email already exists"
}

data class AnsweredQuizDoesNotExist(val id: QuizId) : DomainError {
    val message = "The quiz you have attempted to answer does not exist, ID: $id"
}

sealed interface FieldValidationFailure {
    val message: String
}

data class Empty(val name: String) : FieldValidationFailure {
    override val message = "$name cannot be empty"
}

data class AnswerIndexOutOfBounds(val index: Int, val maxIndex: Int) : FieldValidationFailure {
    override val message = "Answer index is out of bounds, provided $index, max $maxIndex "
}

data class InvalidEmail(val emailAttempt: String) : FieldValidationFailure {
    override val message = "Provided email is not valid: $emailAttempt"
}

sealed interface PersistenceError : DomainError {
    val e: Throwable
}
data class InsertionError(override val e: Throwable): PersistenceError
data class RetrievalError(override val e: Throwable): PersistenceError

fun DomainError.toResponse(errorLens: BiDiBodyLens<ErrorResponse>): Response =
    when (this) {
        is IncorrectFields -> Response(BAD_REQUEST)
            .with(errorLens of ErrorResponse(failures.map { it.message }))

        is PersistenceError -> {
            when (this) {
                is RetrievalError, is InsertionError -> Response(INTERNAL_SERVER_ERROR)
                    .with(errorLens of ErrorResponse(listOf(e.message.orEmpty())))
            }
        }

        is AnsweredQuizDoesNotExist -> Response(NOT_FOUND)
            .with(errorLens of ErrorResponse(listOf(message)))

        is UserAlreadyExists -> Response(BAD_REQUEST)
            .with(errorLens of ErrorResponse(listOf(message)))

    }
