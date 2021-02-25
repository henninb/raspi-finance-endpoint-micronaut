package finance

import io.micronaut.runtime.Micronaut

//@EnableTransactionManagement
open class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Micronaut.run(Application::class.java)
        }
    }
}

