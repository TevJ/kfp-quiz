package com.techtev.quiz

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toEitherNel
import com.techtev.quiz.db.QuizPersistence
import com.techtev.quiz.db.UserPersistence

interface QuizService {
    fun saveQuiz(
        title: String,
        question: String,
        answerIndexes: List<Int>,
        options: List<String>,
        userEmail: String
    ): Either<DomainError, QuizId>

    fun getQuiz(id: QuizId): Either<PersistenceError, Quiz?>

    fun answerQuiz(id: QuizId, answerIndexes: List<Int>): Either<DomainError, AnswerResult>
}

fun quizService(
    quizPersistence: QuizPersistence,
    userPersistence: UserPersistence,
): QuizService = object : QuizService {
    override fun saveQuiz(
        title: String,
        question: String,
        answerIndexes: List<Int>,
        options: List<String>,
        userEmail: String
    ): Either<DomainError, QuizId> = either {
        val user = userPersistence.getUserFromEmail(userEmail).bind()
        val validatedQuiz = Either.zipOrAccumulate(
            Title.from(title).toEitherNel(),
            Text.from(question).toEitherNel(),
            AnswerIndex.from(answerIndexes, options),
            Option.from(options)
        ) { title: Title, text: Text, answer: List<AnswerIndex>, options: List<Option> ->
            Quiz(title = title, text = text, answer = answer, options = options, userId = user?.id?.id)
        }.mapLeft(::IncorrectFields)
            .bind()
        quizPersistence.insertQuiz(validatedQuiz).bind()
    }

    override fun getQuiz(id: QuizId): Either<PersistenceError, Quiz?> =
        quizPersistence.getQuiz(id)

    override fun answerQuiz(id: QuizId, answerIndexes: List<Int>): Either<DomainError, AnswerResult> =
        either {
            val quiz = ensureNotNull(getQuiz(id).bind()) { AnsweredQuizDoesNotExist(id) }
            AnswerResult(quiz.answer.map { it.value }.toSet() == answerIndexes.toSet())
        }
}