package finance.services

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.repositories.FamilyMemberRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import finance.utils.Constants
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class FamilyMemberServiceSpec extends Specification {

    private FamilyMemberRepository familyMemberRepositoryMock = GroovyMock(FamilyMemberRepository)
    private Validator validatorMock = GroovyMock(Validator)
    private MeterRegistry meterRegistryMock = GroovyMock(MeterRegistry)
    private MeterService meterService = new MeterService(meterRegistryMock)
    private FamilyMemberService service = new FamilyMemberService(familyMemberRepositoryMock, validatorMock, meterService)
    private Counter counter = Mock(Counter)

    private static Tag serverNameTag = Tag.of(Constants.SERVER_NAME_TAG, 'localhost')
    private static Tag validationExceptionTag = Tag.of(Constants.EXCEPTION_NAME_TAG, 'ValidationException')
    private static Tags validationExceptionTags = Tags.of(validationExceptionTag, serverNameTag)
    private static Meter.Id validationExceptionThrownMeter = new Meter.Id(Constants.EXCEPTION_THROWN_COUNTER, validationExceptionTags, null, null, Meter.Type.COUNTER)

    private FamilyMember buildMember(Long id = 1L, String owner = 'brian') {
        FamilyMember member = new FamilyMember()
        member.familyMemberId = id
        member.owner = owner
        member.memberName = 'john'
        member.relationship = FamilyRelationship.Self
        member.activeStatus = true
        return member
    }

    void 'test findAllActive - returns active members'() {
        given:
        FamilyMember member = buildMember()

        when:
        List<FamilyMember> results = service.findAllActive()

        then:
        results.size() == 1
        1 * familyMemberRepositoryMock.findByActiveStatusTrue() >> [member]
        0 * _
    }

    void 'test findAllActive - empty list'() {
        when:
        List<FamilyMember> results = service.findAllActive()

        then:
        results.isEmpty()
        1 * familyMemberRepositoryMock.findByActiveStatusTrue() >> []
        0 * _
    }

    void 'test findById - found returns optional'() {
        given:
        FamilyMember member = buildMember()

        when:
        def result = service.findById(1L)

        then:
        result.isPresent()
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(1L) >> member
        0 * _
    }

    void 'test findById - not found returns empty optional'() {
        when:
        def result = service.findById(999L)

        then:
        !result.isPresent()
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(999L) >> null
        0 * _
    }

    void 'test insertFamilyMember - valid member saved successfully'() {
        given:
        FamilyMember member = buildMember()

        when:
        FamilyMember result = service.insertFamilyMember(member)

        then:
        result == member
        1 * validatorMock.validate(member) >> [].toSet()
        1 * familyMemberRepositoryMock.saveAndFlush(member) >> member
        0 * _
    }

    void 'test insertFamilyMember - validation failure throws ValidationException'() {
        given:
        FamilyMember member = buildMember()
        ConstraintViolation<FamilyMember> violation = Stub(ConstraintViolation)
        violation.getMessage() >> 'memberName is invalid'

        when:
        service.insertFamilyMember(member)

        then:
        thrown(ValidationException)
        1 * validatorMock.validate(member) >> [violation].toSet()
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test updateFamilyMember - found and updated returns true'() {
        given:
        FamilyMember existing = buildMember()
        FamilyMember updated = buildMember()
        updated.memberName = 'jane'

        when:
        Boolean result = service.updateFamilyMember(updated)

        then:
        result
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(updated.familyMemberId) >> existing
        1 * familyMemberRepositoryMock.saveAndFlush(updated)
        0 * _
    }

    void 'test updateFamilyMember - not found returns false'() {
        given:
        FamilyMember member = buildMember()

        when:
        Boolean result = service.updateFamilyMember(member)

        then:
        !result
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(member.familyMemberId) >> null
        0 * _
    }

    void 'test deleteByFamilyMemberId - soft deletes and returns true'() {
        when:
        Boolean result = service.deleteByFamilyMemberId(1L)

        then:
        result
        1 * familyMemberRepositoryMock.softDeleteByFamilyMemberId(1L) >> 1
        0 * _
    }

    void 'test deleteByFamilyMemberId - no rows deleted returns false'() {
        when:
        Boolean result = service.deleteByFamilyMemberId(999L)

        then:
        !result
        1 * familyMemberRepositoryMock.softDeleteByFamilyMemberId(999L) >> 0
        0 * _
    }

    void 'test updateActiveStatus - returns true when rows updated'() {
        when:
        Boolean result = service.updateActiveStatus(1L, false)

        then:
        result
        1 * familyMemberRepositoryMock.updateActiveStatus(1L, false) >> 1
        0 * _
    }

    void 'test findByRelationship - filters by relationship'() {
        given:
        FamilyMember self = buildMember(1L, 'brian')
        FamilyMember spouse = buildMember(2L, 'brian')
        spouse.relationship = FamilyRelationship.Spouse

        when:
        List<FamilyMember> results = service.findByRelationship(FamilyRelationship.Self)

        then:
        results.size() == 1
        results[0].relationship == FamilyRelationship.Self
        1 * familyMemberRepositoryMock.findAll() >> [self, spouse]
        0 * _
    }

    void 'test findByOwner - returns members for owner'() {
        given:
        FamilyMember member = buildMember()

        when:
        List<FamilyMember> results = service.findByOwner('brian')

        then:
        results.size() == 1
        1 * familyMemberRepositoryMock.findByOwnerAndActiveStatusTrue('brian') >> [member]
        0 * _
    }

    void 'test findByOwnerAndRelationship - returns filtered members'() {
        given:
        FamilyMember member = buildMember()

        when:
        List<FamilyMember> results = service.findByOwnerAndRelationship('brian', FamilyRelationship.Self)

        then:
        results.size() == 1
        1 * familyMemberRepositoryMock.findByOwnerAndRelationshipAndActiveStatusTrue('brian', FamilyRelationship.Self) >> [member]
        0 * _
    }
}
