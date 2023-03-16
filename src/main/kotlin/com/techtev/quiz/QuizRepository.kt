package com.techtev.quiz

import arrow.core.Either
import arrow.core.raise.either
import com.techtev.quiz.db.QuizPersistence

sealed interface PersistenceError : DomainError {
    val e: Throwable
}
data class InsertionError(override val e: Throwable): PersistenceError
data class RetrievalError(override val e: Throwable): PersistenceError

interface QuizRepository {
    fun createNewQuiz(quiz: Quiz): Either<PersistenceError, QuizId>
    fun getQuiz(id: QuizId): Either<PersistenceError, Quiz?>
}

fun quizRepository(quizPersistence: QuizPersistence) = object : QuizRepository {
    override fun createNewQuiz(
        quiz: Quiz
    ): Either<PersistenceError, QuizId> = quizPersistence.insertQuiz(quiz)

    override fun getQuiz(id: QuizId): Either<PersistenceError, Quiz?> =
        quizPersistence.getQuiz(id)
}