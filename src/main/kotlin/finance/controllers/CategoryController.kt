package finance.controllers

import finance.domain.Category
import finance.services.CategoryService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import java.util.*

@Controller("/api/category")
class CategoryController(@Inject val categoryService: CategoryService) {

    @Get("/active", produces = ["application/json"])
    fun selectAllActiveCategories(): HttpResponse<List<Category>> {
        val categories = categoryService.fetchAllActiveCategories()
        return if (categories.isEmpty()) HttpResponse.notFound() else HttpResponse.ok(categories)
    }

    @Get("/{categoryName}", produces = ["application/json"])
    fun selectCategoryName(@PathVariable categoryName: String): HttpResponse<Category> {
        val optional = categoryService.findByCategoryName(categoryName)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post(produces = ["application/json"])
    fun insertCategory(@Body category: Category): HttpResponse<Category> {
        categoryService.insertCategory(category)
        return HttpResponse.status<Category>(HttpStatus.CREATED).body(category)
    }

    @Put("/{categoryName}", consumes = ["application/json"], produces = ["application/json"])
    fun updateCategory(@PathVariable categoryName: String, @Body category: Category): HttpResponse<Category> {
        category.categoryName = categoryName
        return if (categoryService.updateCategory(category)) HttpResponse.ok(category) else HttpResponse.notFound()
    }

    @Delete("/{categoryName}", produces = ["application/json"])
    fun deleteByCategoryName(@PathVariable categoryName: String): HttpResponse<Category> {
        val optional: Optional<Category> = categoryService.findByCategoryName(categoryName)
        if (optional.isPresent) {
            categoryService.deleteByCategoryName(categoryName)
            return HttpResponse.ok(optional.get())
        }
        return HttpResponse.badRequest()
    }

    @Put("/merge", produces = ["application/json"])
    fun mergeCategories(@QueryValue("new") newCategory: String, @QueryValue("old") oldCategory: String): HttpResponse<Category> {
        return try {
            categoryService.mergeCategories(newCategory, oldCategory)
            val result = categoryService.findByCategoryName(newCategory)
            if (result.isPresent) HttpResponse.ok(result.get()) else HttpResponse.badRequest()
        } catch (e: RuntimeException) {
            HttpResponse.badRequest()
        }
    }
}
