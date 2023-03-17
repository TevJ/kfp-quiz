package com.techtev.quiz

import arrow.core.Either
import com.techtev.quiz.db.UserPersistence

interface UserRepository {
    fun createUser(user: User): Either<PersistenceError, UserId>

    fun getUserFromEmail(email: String): Either<PersistenceError, User?>
}

fun userRepository(userPersistence: UserPersistence) = object : UserRepository {
    override fun createUser(user: User): Either<PersistenceError, UserId> =
        userPersistence.insertUser(user)

    override fun getUserFromEmail(email: String): Either<PersistenceError, User?> =
        userPersistence.getUserFromEmail(email)
}