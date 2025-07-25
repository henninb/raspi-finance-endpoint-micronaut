package finance.services


import finance.domain.User
import finance.repositories.UserRepository
import jakarta.inject.Singleton
import java.security.MessageDigest
import java.util.*


@Singleton
class UserService(
    private val userRepository: UserRepository
) : BaseService() {

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(password.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun verifyPassword(password: String, hashedPassword: String): Boolean {
        return hashPassword(password) == hashedPassword
    }

    fun signIn(user: User): Optional<User> {
        // Retrieve the user by username
        val userOptional = userRepository.findByUsername(user.username)
        if (userOptional.isPresent) {
            val dbUser = userOptional.get()
            // Validate the raw password against the stored hash
            logger.info("user-pass: ${user.password}")
            if (verifyPassword(user.password, dbUser.password)) {
                return Optional.of(user)
            }
        }
        return Optional.empty()
    }

    fun signUp(user: User): User {
        // Check if the username is already taken
        if (userRepository.findByUsername(user.username).isPresent) {
            throw IllegalArgumentException("Username already exists")
        }

        // Hash the raw password securely
        val hashedPassword = hashPassword(user.password)
        user.password = hashedPassword
        return userRepository.saveAndFlush(user)
    }

//    fun findUserByUsername(username: String): User? =
//        userRepository.findByUsername(username).orElse(null)

    fun findUserByUsername(username: String): User? =
        userRepository.findByUsername(username)
            .orElse(null)
            ?.apply { password = "" }
}