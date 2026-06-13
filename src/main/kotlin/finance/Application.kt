package finance

import io.micronaut.core.annotation.Introspected
import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import jakarta.persistence.Entity

@OpenAPIDefinition(
    info = Info(
        title = "raspi-finance-endpoint",
        version = "0.1",
        description = "Personal finance management REST API",
        contact = Contact(name = "Brian Henning")
    )
)
@Introspected(packages = ["finance.domain"], includedAnnotations = [Entity::class])
open class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Micronaut.run(Application::class.java)
        }
    }
}

