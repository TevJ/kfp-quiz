package com.techtev.quiz

import arrow.core.NonEmptyList

sealed interface DomainError

data class IncorrectFields(val failures: NonEmptyList<FieldValidationFailure>) : DomainError {
    val message: String =
        "Issues with the following fields: ${failures.joinToString()}"
}

data class UserAlreadyExists(val email: String) : DomainError {
    val message: String =
        "A user with email $email already exists"
}

data class AnsweredQuizDoesNotExist(val id: Long) : DomainError {
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