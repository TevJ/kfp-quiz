package com.techtev.quiz

import com.techtev.quiz.db.*
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.OpenAPIJackson
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.contract.ui.swaggerUi
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.RequestContextKey
import org.http4k.lens.RequestContextLens
import org.http4k.lens.string
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
    Application().asServer(Jetty(port = 8080)).start()
}

private fun Application(): HttpHandler {
    Database.connect("jdbc:h2:file:./quizdb", "org.h2.Driver", "sa", "password")
    transaction {
        // print sql to std-out
        addLogger(StdOutSqlLogger)

        SchemaUtils.createMissingTablesAndColumns(QuizTable)
        SchemaUtils.create(UserTable)
    }

    val userPersistence = userPersistence(UserTable)
    val userService = userService(userPersistence)
    val contexts = RequestContexts()
    val credentialsKey = RequestContextKey.required<Credentials>(contexts)

    return ServerFilters.InitialiseRequestContext(contexts)
        .then(HandleErrors())
        .then(
            routes(
                QuizApi(credentialsKey, userService, userPersistence),
                UserApi(userService),
                Swagger()
            )
        )
}

private fun Swagger() = swaggerUi(
    descriptionRoute = Uri.of("user"),
    title = "Quiz service",
    displayOperationId = true
)

private fun UserApi(userService: UserService) = contract {
    renderer = OpenApi3(ApiInfo("User service", "v1.0"), OpenAPIJackson)
    descriptionPath = "user"
    routes += userRoutes(userService)
}

private fun QuizApi(
    credentialsKey: RequestContextLens<Credentials>,
    userService: UserService,
    userPersistence: UserPersistence
) = contract {
    renderer = OpenApi3(ApiInfo("Quiz service", "v1.0"), OpenAPIJackson)
    descriptionPath = "quiz"
    security = BasicAuthSecurity("kfp-quiz", credentialsKey, { credentials ->
        userService.isValidUser(credentials.user, credentials.password)
            .fold({ null }, { credentials })
    })
    routes += quizRoutes(
        quizService(quizPersistence(QuizTable), userPersistence),
        credentialsKey
    )
}

private fun HandleErrors() = ServerFilters.CatchLensFailure { failure ->
    Response(BAD_REQUEST).with(
        Body.string(APPLICATION_JSON)
            .toLens() of "{\"message\":${failure.cause?.message.orEmpty()}"
    )
}
