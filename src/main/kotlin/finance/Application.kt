package finance

import io.micronaut.core.annotation.Introspected
import io.micronaut.runtime.Micronaut
import jakarta.persistence.Entity

@Introspected(packages = ["finance.domain"], includedAnnotations = [Entity::class])
open class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Micronaut.run(Application::class.java)
        }
    }
}

