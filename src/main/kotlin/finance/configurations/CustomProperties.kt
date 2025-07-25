package finance.configurations

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("custom.project")
class CustomProperties {
    var excludedAccounts: MutableList<String> = mutableListOf()
    var excelPassword: String = ""
    var excelInputFilePath: String = ""
}