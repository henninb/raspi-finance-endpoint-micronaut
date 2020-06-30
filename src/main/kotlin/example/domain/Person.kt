package example.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.annotations.Proxy
import javax.persistence.*

//@Proxy(lazy = false)
//@Table(name = "person")
@Entity
data class Person(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long,

        @Column(nullable = false)
        var name: String
) {
    constructor() : this(0L, "") {
    }

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}