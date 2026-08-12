package com.cipherchat.server.gateway.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("StatusPages")

fun Application.configureStatusPages() {
    install(StatusPages) {
        // Catch-all for unhandled exceptions — logs the full stack
        // trace server-side but returns only a generic error to the
        // client. Stack traces in API responses are a common
        // information-disclosure vulnerability; this ensures that even
        // a bug in a route handler can't accidentally leak internal
        // service topology, class names, or query fragments to clients.
        exception<Throwable> { call, cause ->
            log.error("Unhandled exception on ${call.request.local.method.value} ${call.request.local.uri}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(code = "internal_error", message = "An unexpected error occurred."),
            )
        }

        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", cause.message ?: "Unauthorized"))
        }

        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ApiError("not_found", cause.message ?: "Not found"))
        }

        exception<ValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError("validation_error", cause.message ?: "Bad request"))
        }

        exception<RateLimitException> { call, _ ->
            call.respond(HttpStatusCode.TooManyRequests, ApiError("rate_limited", "Too many requests — please slow down."))
        }
    }
}

@Serializable
data class ApiError(val code: String, val message: String)

class AuthenticationException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
class ValidationException(message: String) : Exception(message)
class RateLimitException : Exception("Rate limit exceeded")
