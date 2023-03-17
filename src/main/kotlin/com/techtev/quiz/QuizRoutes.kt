package com.techtev.quiz

import kotlinx.serialization.Serializable
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.*

@Serializable
data class QuizRequest(
    val title: String,
    val text: String,
    val options: List<String>,
    val answers: List<Int>
)

@Serializable
data class QuizResponse(
    val id: Long,
    val title: String,
    val text: String,
    val options: List<String>,
    val answers: List<Int>
)

@Serializable
data class ErrorResponse(
    val errors: List<String>
)

@Serializable
data class AnswerQuizRequest(
    val answers: List<Int>
)

@Serializable
data class AnswerQuizResponse(
    val isCorrect: Boolean
)

fun quizRoutes(
    quizService: QuizService
): List<ContractRoute> =
    listOf(
        createQuizRoute(quizService),
        getQuizRoute(quizService),
        answerQuizRoute(quizService)
    )


fun createQuizRoute(
    quizService: QuizService
): ContractRoute {
    val quizRequestBody = Body.auto<QuizRequest>().toLens()
    val quizResponseBody = Body.auto<QuizResponse>().toLens()
    val quizErrorBody = Body.auto<ErrorResponse>().toLens()
    val quizExampleRequest = quizExampleRequest()
    val quizExampleResponse = quizExampleResponse(quizExampleRequest)

    val spec = "/quiz/create" meta {
        summary = "Upload a new quiz"
        receiving(quizRequestBody to quizExampleRequest)
        returning(Status.OK, quizResponseBody to quizExampleResponse)
    } bindContract Method.POST

    val quizHandler: HttpHandler = { request: Request ->
        val receivedQuiz: QuizRequest = quizRequestBody(request)
        quizService.saveQuiz(
            receivedQuiz.title,
            receivedQuiz.text,
            receivedQuiz.answers,
            receivedQuiz.options
        )
            .fold(
                { it.toResponse(quizErrorBody) }
            ) { id ->
                Response(Status.OK).with(quizResponseBody of createQuizResponse(id.value, receivedQuiz))
            }
    }
    return spec to quizHandler
}

fun getQuizRoute(quizService: QuizService): ContractRoute {
    val quizResponseBody = Body.auto<QuizResponse>().toLens()
    val quizErrorBody = Body.auto<ErrorResponse>().toLens()

    val spec = "/quiz" / Path.long().of("id", "Id of quiz to get") meta {
        summary = "Gets a quiz"
        returning(Status.OK, quizResponseBody to quizExampleResponse(quizExampleRequest()))
    } bindContract Method.GET

    fun getQuiz(id: Long): HttpHandler = { _ ->
        quizService.getQuiz(id)
            .fold(
                { e -> e.toResponse(quizErrorBody) },
                { quiz ->
                    quiz?.let {
                        Response(Status.OK).with(quizResponseBody of it.toQuizResponse())
                    } ?: Response(Status.NOT_FOUND)
                }
            )
    }
    return spec to ::getQuiz
}

fun answerQuizRoute(quizService: QuizService): ContractRoute {
    val answerQuizRequestBody = Body.auto<AnswerQuizRequest>().toLens()
    val answerQuizResponseBody = Body.auto<AnswerQuizResponse>().toLens()
    val quizErrorBody = Body.auto<ErrorResponse>().toLens()

    val spec = "/quiz" / Path.long().of("id", "Id of quiz to answer") / "answer" meta {
        summary = "Answer a quiz"
        receiving(answerQuizRequestBody to answerQuizExampleRequest())
        returning(Status.OK, answerQuizResponseBody to answerQuizExampleResponse())
    } bindContract Method.POST

    val answerQuiz: (Long, String) -> HttpHandler = { id: Long, _ ->
        { request: Request ->
            val receivedAnswers: AnswerQuizRequest = answerQuizRequestBody(request)
            quizService.answerQuiz(id, receivedAnswers.answers)
                .fold(
                    { it.toResponse(quizErrorBody) },
                    { answerResult ->
                        Response(Status.OK)
                            .with(answerQuizResponseBody of AnswerQuizResponse(answerResult.isCorrect))
                    }
                )
        }
    }
    return spec to answerQuiz
}

private fun Quiz.toQuizResponse(): QuizResponse =
    QuizResponse(
        id = id.value,
        title = title.value,
        text = text.value,
        options = options.map(Option::value),
        answers = answer.map(AnswerIndex::value)
    )

private fun createQuizResponse(id: Long, quizRequest: QuizRequest): QuizResponse =
    QuizResponse(
        id = id,
        title = quizRequest.title,
        text = quizRequest.text,
        options = quizRequest.options,
        answers = quizRequest.answers
    )

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

private fun quizExampleRequest() = QuizRequest(
    title = "Technology quiz",
    text = "Which of the following programming languages runs on the JVM?",
    options = listOf("Rust", "TypeScript", "Kotlin", "C"),
    answers = listOf(3)
)

private fun quizExampleResponse(quizRequest: QuizRequest) = QuizResponse(
    id = 1,
    title = quizRequest.title,
    text = quizRequest.text,
    options = quizRequest.options,
    answers = quizRequest.answers
)

private fun answerQuizExampleRequest() = AnswerQuizRequest(listOf(2))

private fun answerQuizExampleResponse() = AnswerQuizResponse(true)