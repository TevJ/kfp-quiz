package com.techtev.quiz

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.mindrot.jbcrypt.BCrypt

@JvmInline
value class UserId(val id: Long)

@JvmInline
value class Email(val value: String) {
    companion object {
        private const val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        fun from(email: String): Either<FieldValidationFailure, Email> =
            if (email.matches(emailRegex.toRegex())) {
                Email(email).right()
            } else {
                InvalidEmail(email).left()
            }
    }
}

@JvmInline
value class HashedPassword(val value: String) {
    companion object {
        fun from(password: String): Either<FieldValidationFailure, HashedPassword> =
            if (password.isNotEmpty()) {
                HashedPassword(BCrypt.hashpw(password, BCrypt.gensalt())).right()
            } else {
                Empty("Password").left()
            }
    }
}

data class User(
    val id: UserId = UserId(-1),
    val email: Email,
    val hashedPassword: HashedPassword,
    val quizzes: List<Quiz>?
)