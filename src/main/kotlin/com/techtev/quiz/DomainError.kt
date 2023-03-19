package com.techtev.quiz

import arrow.core.NonEmptyList
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.string

sealed interface DomainError

data class IncorrectFields(val failures: NonEmptyList<FieldValidationFailure>) : DomainError {
    val message: String =
        "Issues with the following fields: ${failures.joinToString()}"
}

data class UserAlreadyExists(val email: Email) : DomainError {
    val message: String =
        "A user with email $email already exists"
}

data class AnsweredQuizDoesNotExist(val id: QuizId) : DomainError {
    val message: String =
        "The quiz you have attempted to answer does not exist, ID: $id"
}

sealed interface FieldValidationFailure {
    val message: String
}

data class Empty(val name: String) : FieldValidationFailure {
    override val message: String =
        "$name cannot be empty"
}

data class AnswerIndexOutOfBounds(val index: Int, val maxIndex: Int) : FieldValidationFailure {
    override val message: String =
        "Answer index is out of bounds, provided $index, max $maxIndex "
}

data class InvalidEmail(val emailAttempt: String) : FieldValidationFailure {
    override val message: String =
        "Provided email is not valid: $emailAttempt"
}

fun DomainError.toResponse(errorLens: BiDiBodyLens<ErrorResponse>): Response =
    when (this) {
        is IncorrectFields ->
            Response(Status.BAD_REQUEST)
                .with(errorLens of ErrorResponse(this.failures.map { it.message }))
        is PersistenceError -> {
            when (this) {
                is RetrievalError, is InsertionError ->
                    Response(Status.INTERNAL_SERVER_ERROR)
                        .with(Body.string(ContentType.TEXT_PLAIN).toLens() of this.e.message.orEmpty())
            }
        }
        is AnsweredQuizDoesNotExist ->
            Response(Status.NOT_FOUND)
                .with(Body.string(ContentType.TEXT_PLAIN).toLens() of this.message)
        is UserAlreadyExists ->
            Response(Status.BAD_REQUEST)
                .with(Body.string(ContentType.TEXT_PLAIN).toLens() of this.message)
    }