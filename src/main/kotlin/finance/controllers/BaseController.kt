package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

open class BaseController {

    companion object {
        val mapper = ObjectMapper().apply {
            findAndRegisterModules()
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
        val logger: Logger = LogManager.getLogger(BaseController::class.java)
    }
}