package finance.repositories

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import jakarta.transaction.Transactional

@Repository
interface FamilyMemberRepository : JpaRepository<FamilyMember, Long> {
    fun findByFamilyMemberIdAndActiveStatusTrue(familyMemberId: Long): FamilyMember?

    fun findByOwnerAndActiveStatusTrue(owner: String): List<FamilyMember>

    fun findByOwnerAndRelationshipAndActiveStatusTrue(
        owner: String,
        relationship: FamilyRelationship,
    ): List<FamilyMember>

    fun findByActiveStatusTrue(): List<FamilyMember>

    fun findByOwnerAndMemberName(owner: String, memberName: String): FamilyMember?

    fun findByOwnerAndFamilyMemberIdAndActiveStatusTrue(owner: String, familyMemberId: Long): FamilyMember?

    fun findByOwnerAndFamilyMemberId(owner: String, familyMemberId: Long): FamilyMember?

    @Transactional
    @Query(
        value = "UPDATE t_family_member SET active_status = false, date_updated = NOW() WHERE family_member_id = :familyMemberId",
        nativeQuery = true,
    )
    fun softDeleteByFamilyMemberId(familyMemberId: Long): Int

    @Transactional
    @Query(
        value = "UPDATE t_family_member SET active_status = false, date_updated = NOW() WHERE family_member_id = :familyMemberId AND owner = :owner",
        nativeQuery = true,
    )
    fun softDeleteByOwnerAndFamilyMemberId(owner: String, familyMemberId: Long): Int

    @Transactional
    @Query(
        value = "UPDATE t_family_member SET active_status = :active, date_updated = NOW() WHERE family_member_id = :familyMemberId",
        nativeQuery = true,
    )
    fun updateActiveStatus(familyMemberId: Long, active: Boolean): Int

    @Transactional
    @Query(
        value = "UPDATE t_family_member SET active_status = :active, date_updated = NOW() WHERE family_member_id = :familyMemberId AND owner = :owner",
        nativeQuery = true,
    )
    fun updateActiveStatusByOwner(owner: String, familyMemberId: Long, active: Boolean): Int
}
