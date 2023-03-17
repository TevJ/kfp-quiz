package com.techtev.quiz.db

import arrow.core.Either
import com.techtev.quiz.*
import com.techtev.quiz.db.UserTable.id
import com.techtev.quiz.db.UserTable.email
import com.techtev.quiz.db.UserTable.hashedPassword
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object UserTable : LongIdTable() {
    val email: Column<String> = text("email")
    val hashedPassword: Column<String> = text("password")
}

interface UserPersistence {
    fun insertUser(user: User): Either<PersistenceError, UserId>

    fun getUserFromEmail(email: String): Either<PersistenceError, User?>
}

fun userPersistence(userTable: UserTable) = object : UserPersistence {
    override fun insertUser(user: User): Either<PersistenceError, UserId> =
        Either.catch {
            transaction {
            UserId(
                userTable.insertAndGetId {
                    it[email] = user.email.value
                    it[hashedPassword] = user.hashedPassword.value
                }.value
            )
            }
        }.mapLeft(::InsertionError)

    override fun getUserFromEmail(email: String): Either<PersistenceError, User?> =
        Either.catch {
            transaction {
                UserTable.select { UserTable.email eq email }
                    .firstOrNull()
                    ?.toUser(null)
            }
        }.mapLeft(::RetrievalError)

    private fun ResultRow.toUser(quizzes: List<Quiz>?): User =
        User(
            id = UserId(this[id].value),
            email = Email(this[email]),
            hashedPassword = HashedPassword(this[hashedPassword]),
            quizzes = quizzes
        )
}