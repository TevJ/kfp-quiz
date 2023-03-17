package com.techtev.quiz

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import org.mindrot.jbcrypt.BCrypt

interface UserService {
    fun registerUser(
        email: String,
        password: String
    ): Either<DomainError, UserId>

    fun isValidUser(
        email: String,
        password: String
    ): Either<DomainError, Boolean>
}

fun userService(
    userRepository: UserRepository
): UserService = object : UserService {
    override fun registerUser(email: String, password: String): Either<DomainError, UserId> =
        either {
            val validatedUser = Either.zipOrAccumulate(
                Email.from(email),
                HashedPassword.from(password)
            ) { email, hashedPassword ->
                User(email = email, hashedPassword = hashedPassword, quizzes = null)
            }.mapLeft(::IncorrectFields)
                .bind()
            val existingUser = userRepository.getUserFromEmail(validatedUser.email.value).bind()
            existingUser?.let {
                raise(UserAlreadyExists(it.email.value))
            } ?: userRepository.createUser(validatedUser).bind()
        }

    override fun isValidUser(email: String, password: String): Either<DomainError, Boolean> =
        either {
            val user = userRepository.getUserFromEmail(email).bind()
            user?.let {
                BCrypt.checkpw(password, it.hashedPassword.value)
            } ?: false
        }
}