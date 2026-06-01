package finance.controllers

import finance.domain.Category
import finance.services.CategoryService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import java.util.*
import jakarta.inject.Inject

@Controller("/category")
class CategoryController(@Inject val categoryService: CategoryService) {

    @Get("/select/active", produces = ["application/json"])
    fun selectAllActiveCategories(): HttpResponse<List<Category>> {
        val categories: List<Category> = categoryService.fetchAllActiveCategories()
        if (categories.isEmpty()) {
            return HttpResponse.notFound()
        }
        return HttpResponse.ok(categories)
    }

    @Get("/select/{categoryName}")
    fun selectCategoryName(@PathVariable categoryName: String): HttpResponse<String> {
        val categoryOptional = categoryService.findByCategoryName(categoryName)
        if (categoryOptional.isPresent) {
            return HttpResponse.ok(BaseController.mapper.writeValueAsString(categoryOptional.get()))
        }
        return HttpResponse.notFound("category not found for: $categoryName")
    }

    @Post("/insert", produces = ["application/json"])
    fun insertCategory(@Body category: Category): HttpResponse<String> {
        categoryService.insertCategory(category)
        return HttpResponse.ok("category inserted")
    }

    @Delete("/delete/{categoryName}", produces = ["application/json"])
    fun deleteByCategoryName(@PathVariable categoryName: String): HttpResponse<String> {
        val categoryOptional: Optional<Category> = categoryService.findByCategoryName(categoryName)
        if (categoryOptional.isPresent) {
            categoryService.deleteByCategoryName(categoryName)
            return HttpResponse.ok("category deleted")
        }
        return HttpResponse.badRequest("could not delete this category: $categoryName.")
    }

    @Put("/update/{categoryName}", consumes = ["application/json"], produces = ["application/json"])
    fun updateCategory(@PathVariable categoryName: String, @Body category: Category): HttpResponse<String> {
        category.categoryName = categoryName
        val updated = categoryService.updateCategory(category)
        return if (updated) HttpResponse.ok("category updated") else HttpResponse.notFound()
    }

    @Put("/merge", produces = ["application/json"])
    fun mergeCategories(@QueryValue("new") newCategory: String, @QueryValue("old") oldCategory: String): HttpResponse<String> {
        return try {
            categoryService.mergeCategories(newCategory, oldCategory)
            HttpResponse.ok("categories merged")
        } catch (e: RuntimeException) {
            HttpResponse.badRequest("could not merge categories: ${e.message}")
        }
    }
}
