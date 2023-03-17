package com.techtev.quiz.db

import arrow.core.Either
import arrow.core.firstOrNone
import com.techtev.quiz.*
import com.techtev.quiz.db.QuizTable.answer
import com.techtev.quiz.db.QuizTable.id
import com.techtev.quiz.db.QuizTable.options
import com.techtev.quiz.db.QuizTable.text
import com.techtev.quiz.db.QuizTable.title
import com.techtev.quiz.db.QuizTable.userId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object QuizTable : LongIdTable() {
    val answer: Column<String> = text("answer")
    val title: Column<String> = text("title")
    val text: Column<String> = text("text")
    val options: Column<String> = text("options")
    val quizId: Column<EntityID<Long>> = id
    val userId = long("user_id")
        .uniqueIndex()
        .references(UserTable.id)
        .nullable()
}

interface QuizPersistence {
    fun insertQuiz(quiz: Quiz): Either<PersistenceError, QuizId>
    fun getQuiz(quizId: QuizId): Either<PersistenceError, Quiz?>
}

fun quizPersistence(quizTable: QuizTable) = object : QuizPersistence {
    override fun insertQuiz(quiz: Quiz): Either<PersistenceError, QuizId> =
        Either.catch {
            transaction {
                QuizId(
                    quizTable.insertAndGetId {
                        it[answer] = quiz.answer.joinToString { a -> a.value.toString() }
                        it[title] = quiz.title.value
                        it[text] = quiz.text.value
                        it[options] = quiz.options.joinToString(separator = "\\~") { o -> o.value }
                    }.value
                )
            }
        }.mapLeft(::InsertionError)

    override fun getQuiz(quizId: QuizId): Either<PersistenceError, Quiz?> =
        Either.catch {
            transaction {
                QuizTable.select { QuizTable.quizId eq quizId.value }
                    .firstOrNull()
                    ?.toQuiz()
            }
        }.mapLeft(::RetrievalError)

    private fun ResultRow.toQuiz(): Quiz =
        Quiz(
            id = QuizId(this[id].value),
            title = Title(this[title]),
            text = Text(this[text]),
            answer = this[answer].split(",").map { AnswerIndex(it.toInt()) },
            options = this[options].split("\\~").map { Option(it) },
            userId = this[userId]
        )
}