package finance.controllers

import finance.domain.Category
import finance.services.CategoryService
import finance.services.DescriptionService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import java.util.*
import javax.inject.Inject

@Controller("/category")
class CategoryController(@Inject val categoryService: CategoryService) {
    //http://localhost:8080/category/select/active
    @Get("/select/active", produces = ["application/json"])
    fun selectAllActiveCategories(): HttpResponse<List<Category>> {
        val categories: List<Category> = categoryService.fetchAllActiveCategories()
        if (categories.isEmpty()) {
            return HttpResponse.notFound()
            //BaseController.logger.error("no categories found in the datastore.")
            //throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find any categories in the datastore.")
        }
        BaseController.logger.info("select active categories: ${categories.size}")
        return HttpResponse.ok(categories)
    }

    @Get("/select/{category_name}")
    fun selectCategoryName(@PathVariable("category_name") categoryName: String): HttpResponse<String> {
        val categoryOptional = categoryService.findByCategory(categoryName)
        if (categoryOptional.isPresent) {
            return HttpResponse.ok(BaseController.mapper.writeValueAsString(categoryOptional.get()))
        }
        return HttpResponse.notFound("category not found for: $categoryName")
        //throw ResponseStatusException(HttpStatus.NOT_FOUND, "category not found for: $categoryName")
    }

    //curl --header "Content-Type: application/json" -X POST -d '{"category":"test"}' http://localhost:8080/category/insert
    @Post("/insert", produces = ["application/json"])
    fun insertCategory(@Body category: Category): HttpResponse<String> {
        categoryService.insertCategory(category)
        //BaseController.logger.info("insertCategory")
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
        //throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not delete this category: $categoryName.")
    }
}