package finance.controllers

import finance.services.ExcelFileService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.exceptions.HttpStatusException

@Controller("/excel")
class ExcelFileController(private val excelFileService: ExcelFileService) : BaseController() {

    // curl -k https://localhost:8443/excel/file/export
    @Get("/file/export")
    @Produces("application/json")
    fun exportExcelFile(): HttpResponse<String> {
        return try {
            logger.info("Processing protected excel file: finance_db_master.xlsm")
            excelFileService.processProtectedExcelFile("finance_db_master.xlsm")
            logger.info("Excel file processed successfully")
            HttpResponse.ok("Excel file processed successfully")
        } catch (ex: Exception) {
            logger.error("Failed to process excel file: ${ex.message}", ex)
            throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process excel file: ${ex.message}")
        }
    }
}