package finance.services


import finance.domain.User
import finance.repositories.UserRepository
import jakarta.inject.Singleton
import org.mindrot.jbcrypt.BCrypt
import java.util.*


@Singleton
open class UserService(
    private val userRepository: UserRepository
) : BaseService() {

    open fun signIn(user: User): Optional<User> {
        val userOptional = userRepository.findByUsername(user.username)
        if (userOptional.isPresent) {
            val dbUser = userOptional.get()
            if (BCrypt.checkpw(user.password, dbUser.password)) {
                return Optional.of(user)
            }
        }
        return Optional.empty()
    }

    open fun signUp(user: User): User {
        if (userRepository.findByUsername(user.username).isPresent) {
            throw IllegalArgumentException("Username already exists")
        }
        user.password = BCrypt.hashpw(user.password, BCrypt.gensalt(12))
        return userRepository.saveAndFlush(user)
    }

//    fun findUserByUsername(username: String): User? =
//        userRepository.findByUsername(username).orElse(null)

    open fun findUserByUsername(username: String): User? =
        userRepository.findByUsername(username)
            .orElse(null)
            ?.apply { password = "" }
}