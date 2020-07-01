package example.services

import example.domain.Account
import example.repositories.AccountRepository
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.ConstraintViolation
import javax.validation.Validator

@Singleton
class AccountService(@Inject val accountRepository: AccountRepository, @Inject val validator: Validator) {
    fun insertAccount(account: Account): Boolean {
        val constraintViolations: Set<ConstraintViolation<Account>> = validator.validate(account)
        val accountNameOwnerOptional = findByAccountNameOwner(account.accountNameOwner)
        if (constraintViolations.isNotEmpty()) {
            //TODO: handle the violation
            //logger.info("constraint issue.")
            return false
        }
        //TODO: Should saveAndFlush be in a try catch block?
        //logger.info("INFO: transactionRepository.saveAndFlush call.")
        if( !accountNameOwnerOptional.isPresent) {
            accountRepository.save(account)
        }
        //logger.info("INFO: transactionRepository.saveAndFlush success.")
        return true
    }

    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account> {
        return accountRepository.findByAccountNameOwner(accountNameOwner)
    }
}