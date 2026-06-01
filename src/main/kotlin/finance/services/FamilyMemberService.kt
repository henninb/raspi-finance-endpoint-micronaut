package finance.services

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.repositories.FamilyMemberRepository
import io.micrometer.core.annotation.Timed
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.apache.logging.log4j.LogManager
import java.sql.Timestamp
import java.util.Calendar
import java.util.Optional

@Singleton
open class FamilyMemberService(
    @Inject val familyMemberRepository: FamilyMemberRepository,
    @Inject val validator: Validator,
    @Inject val meterService: MeterService,
) {
    @Timed
    open fun findAllActive(): List<FamilyMember> = familyMemberRepository.findByActiveStatusTrue()

    @Timed
    open fun findById(id: Long): Optional<FamilyMember> =
        Optional.ofNullable(familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(id))

    @Timed
    open fun insertFamilyMember(member: FamilyMember): FamilyMember {
        val constraintViolations: Set<ConstraintViolation<FamilyMember>> = validator.validate(member)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { logger.error(it.message) }
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert family member due to constraint violation.")
        }
        member.dateAdded = Timestamp(Calendar.getInstance().time.time)
        member.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        return familyMemberRepository.saveAndFlush(member)
    }

    @Timed
    open fun updateFamilyMember(member: FamilyMember): Boolean {
        val existing = familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(member.familyMemberId)
            ?: return false
        member.dateAdded = existing.dateAdded
        member.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        familyMemberRepository.saveAndFlush(member)
        return true
    }

    @Timed
    open fun deleteByFamilyMemberId(id: Long): Boolean {
        val rows = familyMemberRepository.softDeleteByFamilyMemberId(id)
        return rows > 0
    }

    @Timed
    open fun findByRelationship(relationship: FamilyRelationship): List<FamilyMember> =
        familyMemberRepository.findAll().filter { it.relationship == relationship && it.activeStatus }

    @Timed
    open fun updateActiveStatus(id: Long, active: Boolean): Boolean {
        val rows = familyMemberRepository.updateActiveStatus(id, active)
        return rows > 0
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
