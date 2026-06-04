package finance.helpers

import finance.domain.Account
import finance.domain.AccountType

class AccountBuilder {

    String accountNameOwner = 'foo_brian'
    AccountType accountType = AccountType.Credit
    Boolean activeStatus = true
    String moniker = '0000'
    BigDecimal outstanding = new BigDecimal(0)
    BigDecimal future = new BigDecimal(0)
    BigDecimal cleared = new BigDecimal(0)

    static AccountBuilder builder() {
        return new AccountBuilder()
    }

    Account build() {
        Account account = new Account().with {
            accountNameOwner = this.accountNameOwner
            accountType = this.accountType
            activeStatus = this.activeStatus
            moniker = this.moniker
            outstanding = this.outstanding
            future = this.future
            cleared = this.cleared
            return it
        }
        return account
    }

    AccountBuilder withAccountNameOwner(String accountNameOwner) {
        this.accountNameOwner = accountNameOwner
        return this
    }

    AccountBuilder withAccountType(AccountType accountType) {
        this.accountType = accountType
        return this
    }

    AccountBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    AccountBuilder withMoniker(String moniker) {
        this.moniker = moniker
        return this
    }

    AccountBuilder withOutstanding(BigDecimal outstanding) {
        this.outstanding = outstanding
        return this
    }

    AccountBuilder withFuture(BigDecimal future) {
        this.future = future
        return this
    }

    AccountBuilder withCleared(BigDecimal cleared) {
        this.cleared = cleared
        return this
    }

    // Legacy aliases kept for backward compatibility
    AccountBuilder withTotals(BigDecimal totals) {
        this.outstanding = totals
        return this
    }

    AccountBuilder withTotalsBalanced(BigDecimal totalsBalanced) {
        this.cleared = totalsBalanced
        return this
    }
}
