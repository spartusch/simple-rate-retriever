package com.github.spartusch.rateretriever.infrastructure.api

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.Locale
import javax.servlet.http.HttpServletRequest

private val log = LoggerFactory.getLogger(ControllerExceptionHandler::class.java)

@RestControllerAdvice
class ControllerExceptionHandler {

    private fun logAndGetMessage(e: RuntimeException, request: HttpServletRequest): String {
        log.error("Exception processing request {}", request.requestURI, e)
        val cause = if (e.cause == null) e else e.cause!!
        return String.format(Locale.US, "%s: %s", cause.javaClass.simpleName, e.message)
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(e: IllegalArgumentException, request: HttpServletRequest) = logAndGetMessage(e, request)

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(e: RuntimeException, request: HttpServletRequest) = logAndGetMessage(e, request)
}
