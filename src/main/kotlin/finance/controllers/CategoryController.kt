package finance.controllers

import finance.domain.Category
import finance.services.CategoryService
import finance.services.OwnerExtractorService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import java.util.*

@Controller("/api/category")
class CategoryController(
    @Inject val categoryService: CategoryService,
    @Inject val ownerExtractorService: OwnerExtractorService,
) {

    @Get("/active", produces = ["application/json"])
    fun selectAllActiveCategories(request: HttpRequest<*>): HttpResponse<List<Category>> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        val categories = categoryService.fetchAllActiveCategories(owner)
        return if (categories.isEmpty()) HttpResponse.notFound() else HttpResponse.ok(categories)
    }

    @Get("/{categoryName}", produces = ["application/json"])
    fun selectCategoryName(@PathVariable categoryName: String, request: HttpRequest<*>): HttpResponse<Category> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        val optional = categoryService.findByOwnerAndCategoryName(owner, categoryName)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post(produces = ["application/json"])
    fun insertCategory(@Body category: Category, request: HttpRequest<*>): HttpResponse<Category> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (category.owner.isBlank()) category.owner = owner
        categoryService.insertCategory(category)
        return HttpResponse.status<Category>(HttpStatus.CREATED).body(category)
    }

    @Put("/{categoryName}", consumes = ["application/json"], produces = ["application/json"])
    fun updateCategory(@PathVariable categoryName: String, @Body category: Category, request: HttpRequest<*>): HttpResponse<Category> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        category.categoryName = categoryName
        if (category.owner.isBlank()) category.owner = owner
        return if (categoryService.updateCategory(category)) HttpResponse.ok(category) else HttpResponse.notFound()
    }

    @Delete("/{categoryName}", produces = ["application/json"])
    fun deleteByCategoryName(@PathVariable categoryName: String, request: HttpRequest<*>): HttpResponse<Category> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        val optional: Optional<Category> = categoryService.findByOwnerAndCategoryName(owner, categoryName)
        if (optional.isPresent) {
            categoryService.deleteByCategoryName(owner, categoryName)
            return HttpResponse.ok(optional.get())
        }
        return HttpResponse.notFound()
    }

    @Put("/merge", produces = ["application/json"])
    fun mergeCategories(@QueryValue("new") newCategory: String, @QueryValue("old") oldCategory: String, request: HttpRequest<*>): HttpResponse<Category> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        return try {
            categoryService.mergeCategories(newCategory, oldCategory)
            val result = categoryService.findByOwnerAndCategoryName(owner, newCategory)
            if (result.isPresent) HttpResponse.ok(result.get()) else HttpResponse.badRequest()
        } catch (e: RuntimeException) {
            HttpResponse.badRequest()
        }
    }
}
