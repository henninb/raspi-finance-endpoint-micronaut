package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.services.FamilyMemberService
import finance.services.OwnerExtractorService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class FamilyMemberControllerSpec extends Specification {

    private FamilyMemberService familyMemberServiceMock = GroovyMock(FamilyMemberService)
    private OwnerExtractorService ownerExtractorServiceMock = GroovyMock(OwnerExtractorService)
    private FamilyMemberController controller = new FamilyMemberController(familyMemberServiceMock, ownerExtractorServiceMock)
    private HttpRequest requestMock = Mock(HttpRequest)

    private FamilyMember buildMember(Long id = 1L, String owner = 'brian') {
        FamilyMember member = new FamilyMember()
        member.familyMemberId = id
        member.owner = owner
        member.memberName = 'john'
        member.relationship = FamilyRelationship.Self
        member.activeStatus = true
        return member
    }

    void 'test selectAllActive - returns 200 with members'() {
        given:
        FamilyMember member = buildMember()

        when:
        HttpResponse response = controller.selectAllActive()

        then:
        response.status == HttpStatus.OK
        1 * familyMemberServiceMock.findAllActive() >> [member]
        0 * _
    }

    void 'test selectAllActive - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectAllActive()

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * familyMemberServiceMock.findAllActive() >> []
        0 * _
    }

    void 'test selectById - returns 200 when found'() {
        given:
        FamilyMember member = buildMember()

        when:
        HttpResponse response = controller.selectById(1L)

        then:
        response.status == HttpStatus.OK
        1 * familyMemberServiceMock.findById(1L) >> Optional.of(member)
        0 * _
    }

    void 'test selectById - returns 404 when not found'() {
        when:
        HttpResponse response = controller.selectById(999L)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * familyMemberServiceMock.findById(999L) >> Optional.empty()
        0 * _
    }

    void 'test insertFamilyMember - returns 401 when no owner'() {
        given:
        FamilyMember member = buildMember()

        when:
        HttpResponse response = controller.insertFamilyMember(member, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test insertFamilyMember - returns 201 on success'() {
        given:
        FamilyMember member = buildMember()

        when:
        HttpResponse response = controller.insertFamilyMember(member, requestMock)

        then:
        response.status == HttpStatus.CREATED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * familyMemberServiceMock.insertFamilyMember(member)
        0 * _
    }

    void 'test updateFamilyMember - returns 401 when no owner'() {
        given:
        FamilyMember member = buildMember()

        when:
        HttpResponse response = controller.updateFamilyMember(1L, member, requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> null
        0 * _
    }

    void 'test updateFamilyMember - returns 200 on success'() {
        given:
        FamilyMember member = buildMember()

        when:
        HttpResponse response = controller.updateFamilyMember(1L, member, requestMock)

        then:
        response.status == HttpStatus.OK
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * familyMemberServiceMock.updateFamilyMember(member) >> true
        0 * _
    }

    void 'test updateFamilyMember - returns 404 when not found'() {
        given:
        FamilyMember member = buildMember()

        when:
        HttpResponse response = controller.updateFamilyMember(1L, member, requestMock)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * ownerExtractorServiceMock.extractOwner(requestMock) >> 'brian'
        1 * familyMemberServiceMock.updateFamilyMember(member) >> false
        0 * _
    }

    void 'test deleteByFamilyMemberId - returns 200 when found'() {
        given:
        FamilyMember member = buildMember()

        when:
        HttpResponse response = controller.deleteByFamilyMemberId(1L)

        then:
        response.status == HttpStatus.OK
        1 * familyMemberServiceMock.findById(1L) >> Optional.of(member)
        1 * familyMemberServiceMock.deleteByFamilyMemberId(1L)
        0 * _
    }

    void 'test deleteByFamilyMemberId - returns 404 when not found'() {
        when:
        HttpResponse response = controller.deleteByFamilyMemberId(999L)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * familyMemberServiceMock.findById(999L) >> Optional.empty()
        0 * _
    }

    void 'test selectByOwner - returns 200 with members'() {
        given:
        FamilyMember member = buildMember()

        when:
        HttpResponse response = controller.selectByOwner('brian')

        then:
        response.status == HttpStatus.OK
        1 * familyMemberServiceMock.findByOwner('brian') >> [member]
        0 * _
    }

    void 'test selectByOwner - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectByOwner('notfound')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * familyMemberServiceMock.findByOwner('notfound') >> []
        0 * _
    }

    void 'test selectByOwnerAndRelationship - returns 200 with members'() {
        given:
        FamilyMember member = buildMember()

        when:
        HttpResponse response = controller.selectByOwnerAndRelationship('brian', FamilyRelationship.Self)

        then:
        response.status == HttpStatus.OK
        1 * familyMemberServiceMock.findByOwnerAndRelationship('brian', FamilyRelationship.Self) >> [member]
        0 * _
    }

    void 'test selectByOwnerAndRelationship - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectByOwnerAndRelationship('brian', FamilyRelationship.Spouse)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * familyMemberServiceMock.findByOwnerAndRelationship('brian', FamilyRelationship.Spouse) >> []
        0 * _
    }

    void 'test activateFamilyMember - returns 200 when updated'() {
        when:
        HttpResponse response = controller.activateFamilyMember(1L)

        then:
        response.status == HttpStatus.OK
        1 * familyMemberServiceMock.updateActiveStatus(1L, true) >> true
        0 * _
    }

    void 'test activateFamilyMember - returns 404 when not found'() {
        when:
        HttpResponse response = controller.activateFamilyMember(999L)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * familyMemberServiceMock.updateActiveStatus(999L, true) >> false
        0 * _
    }

    void 'test deactivateFamilyMember - returns 200 when updated'() {
        when:
        HttpResponse response = controller.deactivateFamilyMember(1L)

        then:
        response.status == HttpStatus.OK
        1 * familyMemberServiceMock.updateActiveStatus(1L, false) >> true
        0 * _
    }

    void 'test deactivateFamilyMember - returns 404 when not found'() {
        when:
        HttpResponse response = controller.deactivateFamilyMember(999L)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * familyMemberServiceMock.updateActiveStatus(999L, false) >> false
        0 * _
    }
}
