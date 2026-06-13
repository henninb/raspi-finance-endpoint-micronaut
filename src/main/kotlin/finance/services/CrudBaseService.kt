package finance.services

import finance.domain.ServiceResult
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory

abstract class CrudBaseService<T, ID> : BaseService() {

    private val crudLogger = LoggerFactory.getLogger(this::class.java)

    protected fun <R> handleServiceOperation(operationName: String, block: () -> R): ServiceResult<R> {
        return try {
            val result = block()
            ServiceResult.Success(result)
        } catch (e: NoSuchElementException) {
            crudLogger.warn("NOT_FOUND op={} message={}", operationName, e.message)
            ServiceResult.NotFound(e.message ?: "Resource not found")
        } catch (e: ConstraintViolationException) {
            crudLogger.error("VALIDATION_ERROR op={}", operationName, e)
            val errors = e.constraintViolations.associate {
                (it.propertyPath?.toString() ?: "field") to (it.message ?: "invalid")
            }
            ServiceResult.ValidationError(errors)
        } catch (e: IllegalArgumentException) {
            crudLogger.error("BUSINESS_ERROR op={} message={}", operationName, e.message)
            ServiceResult.BusinessError(e.message ?: "Business rule violation", "BUSINESS_ERROR")
        } catch (e: IllegalStateException) {
            crudLogger.error("BUSINESS_ERROR op={} message={}", operationName, e.message)
            ServiceResult.BusinessError(e.message ?: "Invalid state", "INVALID_STATE")
        } catch (e: Exception) {
            crudLogger.error("SYSTEM_ERROR op={}", operationName, e)
            ServiceResult.SystemError(e)
        }
    }

    abstract fun findById(id: ID): ServiceResult<T>
    abstract fun findAll(): ServiceResult<List<T>>
    abstract fun save(entity: T): ServiceResult<T>
    abstract fun deleteById(id: ID): ServiceResult<Boolean>
}
