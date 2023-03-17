# Kotlin Functional Quiz

This repository contains an exploratory project that I've written using [Http4k](https://github.com/http4k/http4k), 
[Arrow](https://github.com/arrow-kt/arrow) and [Exposed](https://github.com/JetBrains/Exposed).
The project is intended to be written in a functional style.

## Running the project

The easiest way to run the project is to clone the repository and then import the project into
Intellij IDEA and run the `main()` function in `Application.kt`.

You can also build the `jar` using `.gradlew jar` and then run the jar with java.

## Functionality

### Http4k Contract routes with Open API v3

The project utilises Http4k's contract routes functionality to provide type-safe routes
with automatically generated Open API v3 documentation.

When running the project locally, the documentation can be found at http://127.0.0.1:8080/#/

### Validation and error handling with Arrow

The project utilises Arrow 2.0's Either class to achieve validation and error handling.

### Database persistence with Exposed

The project uses Jetbrains Exposed framework to work with persistence.
This project uses an H2 database file.