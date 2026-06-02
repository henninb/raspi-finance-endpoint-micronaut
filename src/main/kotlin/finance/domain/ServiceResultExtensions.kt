package finance.domain

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

fun <T : Any> ServiceResult<T>.toOkResponse(): HttpResponse<T> =
    when (this) {
        is ServiceResult.Success -> HttpResponse.ok(data)
        is ServiceResult.NotFound -> HttpResponse.notFound()
        is ServiceResult.ValidationError -> HttpResponse.badRequest()
        is ServiceResult.BusinessError -> HttpResponse.status(HttpStatus.CONFLICT)
        is ServiceResult.SystemError -> HttpResponse.serverError()
    }

fun <T : Any> ServiceResult<T>.toCreatedResponse(): HttpResponse<T> =
    when (this) {
        is ServiceResult.Success -> HttpResponse.status<T>(HttpStatus.CREATED).body(data)
        is ServiceResult.NotFound -> HttpResponse.notFound()
        is ServiceResult.ValidationError -> HttpResponse.badRequest()
        is ServiceResult.BusinessError -> HttpResponse.status(HttpStatus.CONFLICT)
        is ServiceResult.SystemError -> HttpResponse.serverError()
    }

fun <T : Any> ServiceResult<List<T>>.toListOkResponse(): HttpResponse<List<T>> =
    when (this) {
        is ServiceResult.Success -> HttpResponse.ok(data)
        is ServiceResult.NotFound -> HttpResponse.notFound()
        is ServiceResult.ValidationError -> HttpResponse.serverError()
        is ServiceResult.BusinessError -> HttpResponse.serverError()
        is ServiceResult.SystemError -> HttpResponse.serverError()
    }
