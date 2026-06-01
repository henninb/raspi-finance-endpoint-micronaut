package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.services.FamilyMemberService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import jakarta.inject.Inject

@Controller("/family-member")
class FamilyMemberController(@Inject val familyMemberService: FamilyMemberService) {

    @Get("/select/active", produces = ["application/json"])
    fun selectAllActive(): HttpResponse<List<FamilyMember>> {
        val members = familyMemberService.findAllActive()
        return if (members.isEmpty()) HttpResponse.notFound() else HttpResponse.ok(members)
    }

    @Get("/select/{familyMemberId}", produces = ["application/json"])
    fun selectById(@PathVariable familyMemberId: Long): HttpResponse<FamilyMember> {
        val optional = familyMemberService.findById(familyMemberId)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertFamilyMember(@Body member: FamilyMember): HttpResponse<String> {
        familyMemberService.insertFamilyMember(member)
        return HttpResponse.ok("family member inserted")
    }

    @Put("/update/{familyMemberId}", consumes = ["application/json"], produces = ["application/json"])
    fun updateFamilyMember(
        @PathVariable familyMemberId: Long,
        @Body member: FamilyMember,
    ): HttpResponse<String> {
        member.familyMemberId = familyMemberId
        val updated = familyMemberService.updateFamilyMember(member)
        return if (updated) HttpResponse.ok("family member updated") else HttpResponse.notFound()
    }

    @Delete("/delete/{familyMemberId}", produces = ["application/json"])
    fun deleteByFamilyMemberId(@PathVariable familyMemberId: Long): HttpResponse<String> {
        val deleted = familyMemberService.deleteByFamilyMemberId(familyMemberId)
        return if (deleted) HttpResponse.ok("family member deleted") else HttpResponse.notFound()
    }

    @Get("/select/relationship/{relationship}", produces = ["application/json"])
    fun selectByRelationship(@PathVariable relationship: FamilyRelationship): HttpResponse<List<FamilyMember>> {
        val members = familyMemberService.findByRelationship(relationship)
        return HttpResponse.ok(members)
    }

    @Put("/activate/{familyMemberId}", produces = ["application/json"])
    fun activateFamilyMember(@PathVariable familyMemberId: Long): HttpResponse<String> {
        val updated = familyMemberService.updateActiveStatus(familyMemberId, true)
        return if (updated) HttpResponse.ok("family member activated") else HttpResponse.notFound()
    }

    @Put("/deactivate/{familyMemberId}", produces = ["application/json"])
    fun deactivateFamilyMember(@PathVariable familyMemberId: Long): HttpResponse<String> {
        val updated = familyMemberService.updateActiveStatus(familyMemberId, false)
        return if (updated) HttpResponse.ok("family member deactivated") else HttpResponse.notFound()
    }
}
