package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.services.FamilyMemberService
import finance.services.OwnerExtractorService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject

@Controller("/api/family-members")
class FamilyMemberController(
    @Inject val familyMemberService: FamilyMemberService,
    @Inject val ownerExtractorService: OwnerExtractorService,
) {

    @Get("/active", produces = ["application/json"])
    fun selectAllActive(): HttpResponse<List<FamilyMember>> {
        val members = familyMemberService.findAllActive()
        return if (members.isEmpty()) HttpResponse.notFound() else HttpResponse.ok(members)
    }

    @Get("/{familyMemberId}", produces = ["application/json"])
    fun selectById(@PathVariable familyMemberId: Long): HttpResponse<FamilyMember> {
        val optional = familyMemberService.findById(familyMemberId)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post(consumes = ["application/json"], produces = ["application/json"])
    fun insertFamilyMember(@Body member: FamilyMember, request: HttpRequest<*>): HttpResponse<FamilyMember> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (member.owner.isBlank()) member.owner = owner
        familyMemberService.insertFamilyMember(member)
        return HttpResponse.status<FamilyMember>(HttpStatus.CREATED).body(member)
    }

    @Put("/{familyMemberId}", consumes = ["application/json"], produces = ["application/json"])
    fun updateFamilyMember(@PathVariable familyMemberId: Long, @Body member: FamilyMember, request: HttpRequest<*>): HttpResponse<FamilyMember> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        member.familyMemberId = familyMemberId
        if (member.owner.isBlank()) member.owner = owner
        return if (familyMemberService.updateFamilyMember(member)) HttpResponse.ok(member) else HttpResponse.notFound()
    }

    @Delete("/{familyMemberId}", produces = ["application/json"])
    fun deleteByFamilyMemberId(@PathVariable familyMemberId: Long): HttpResponse<FamilyMember> {
        val optional = familyMemberService.findById(familyMemberId)
        if (optional.isPresent) {
            familyMemberService.deleteByFamilyMemberId(familyMemberId)
            return HttpResponse.ok(optional.get())
        }
        return HttpResponse.notFound()
    }

    @Get("/owner/{owner}", produces = ["application/json"])
    fun selectByOwner(@PathVariable owner: String): HttpResponse<List<FamilyMember>> {
        val members = familyMemberService.findByOwner(owner)
        return if (members.isEmpty()) HttpResponse.notFound() else HttpResponse.ok(members)
    }

    @Get("/owner/{owner}/relationship/{relationship}", produces = ["application/json"])
    fun selectByOwnerAndRelationship(
        @PathVariable owner: String,
        @PathVariable relationship: FamilyRelationship
    ): HttpResponse<List<FamilyMember>> {
        val members = familyMemberService.findByOwnerAndRelationship(owner, relationship)
        return if (members.isEmpty()) HttpResponse.notFound() else HttpResponse.ok(members)
    }

    @Put("/{id}/activate", produces = ["application/json"])
    fun activateFamilyMember(@PathVariable id: Long): HttpResponse<Map<String, String>> {
        return if (familyMemberService.updateActiveStatus(id, true))
            HttpResponse.ok(mapOf("message" to "family member activated"))
        else HttpResponse.notFound()
    }

    @Put("/{id}/deactivate", produces = ["application/json"])
    fun deactivateFamilyMember(@PathVariable id: Long): HttpResponse<Map<String, String>> {
        return if (familyMemberService.updateActiveStatus(id, false))
            HttpResponse.ok(mapOf("message" to "family member deactivated"))
        else HttpResponse.notFound()
    }
}
