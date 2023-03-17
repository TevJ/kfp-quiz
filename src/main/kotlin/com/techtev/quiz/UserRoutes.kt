package com.techtev.quiz

import kotlinx.serialization.Serializable
import org.http4k.contract.ContractRoute
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.format.Jackson.auto

@Serializable
data class RegisterUserRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterUserResponse(
    val email: String,
    val id: Long
)

fun userRoutes(
    userService: UserService
): List<ContractRoute> = listOf(registerUserRoute(userService))

fun registerUserRoute(
    userService: UserService
): ContractRoute {
    val registerUserRequestBody = Body.auto<RegisterUserRequest>().toLens()
    val registerUserResponseBody = Body.auto<RegisterUserResponse>().toLens()
    val errorResponseBody = Body.auto<ErrorResponse>().toLens()
    val registerUserExampleRequest = registerUserExampleRequest()
    val registerUserExampleResponse = registerUserResponseExample(registerUserExampleRequest)

    val spec = "/register" meta {
        summary = "Register a new user"
        receiving(registerUserRequestBody to registerUserExampleRequest)
        returning(Status.OK, registerUserResponseBody to registerUserExampleResponse)
    } bindContract Method.POST

    val registerUserHandler: HttpHandler = { request: Request ->
        val receivedUser: RegisterUserRequest = registerUserRequestBody(request)
        userService.registerUser(
            receivedUser.email,
            receivedUser.password
        )
            .fold(
                { it.toResponse(errorResponseBody) },
                { id ->
                    Response(Status.OK)
                        .with(registerUserResponseBody of RegisterUserResponse(receivedUser.email, id.id))
                }
            )
    }

    return spec to registerUserHandler
}

private fun registerUserExampleRequest() = RegisterUserRequest(
    email = "your.email@gmail.com",
    password = "very-secure-password"
)

private fun registerUserResponseExample(registerUserRequest: RegisterUserRequest) =
    RegisterUserResponse(
        email = registerUserRequest.email,
        id = 1
    )