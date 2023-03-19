package com.techtev.quiz

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import org.mindrot.jbcrypt.BCrypt

@JvmInline
value class UserId(val id: Long)

@JvmInline
value class Email(val value: String) {
    companion object {
        private const val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        fun from(email: String): Either<FieldValidationFailure, Email> =
            either {
                ensure(email.matches(emailRegex.toRegex())) { InvalidEmail(email) }
                Email(email)
            }
    }
}

@JvmInline
value class HashedPassword(val value: String) {
    companion object {
        fun from(password: String): Either<FieldValidationFailure, HashedPassword> =
            either {
                ensure(password.isNotEmpty()) { Empty("Password") }
                HashedPassword(BCrypt.hashpw(password, BCrypt.gensalt()))
            }
    }
}

data class User(
    val id: UserId = UserId(-1),
    val email: Email,
    val hashedPassword: HashedPassword,
    val quizzes: List<Quiz>?
)