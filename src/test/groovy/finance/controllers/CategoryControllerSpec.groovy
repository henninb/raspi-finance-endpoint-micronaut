package finance.controllers

import finance.domain.Category
import finance.helpers.CategoryBuilder
import finance.services.CategoryService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class CategoryControllerSpec extends Specification {

    private CategoryService categoryServiceMock = GroovyMock(CategoryService)
    private CategoryController controller = new CategoryController(categoryServiceMock)

    void 'test selectAllActiveCategories - returns 200 with categories'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        HttpResponse response = controller.selectAllActiveCategories()

        then:
        response.status == HttpStatus.OK
        1 * categoryServiceMock.fetchAllActiveCategories() >> [category]
        0 * _
    }

    void 'test selectAllActiveCategories - returns 404 when empty'() {
        when:
        HttpResponse response = controller.selectAllActiveCategories()

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * categoryServiceMock.fetchAllActiveCategories() >> []
        0 * _
    }

    void 'test selectCategoryName - returns 200 when found'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        HttpResponse response = controller.selectCategoryName('online')

        then:
        response.status == HttpStatus.OK
        1 * categoryServiceMock.findByCategoryName('online') >> Optional.of(category)
        0 * _
    }

    void 'test selectCategoryName - returns 404 when not found'() {
        when:
        HttpResponse response = controller.selectCategoryName('notfound')

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * categoryServiceMock.findByCategoryName('notfound') >> Optional.empty()
        0 * _
    }

    void 'test insertCategory - returns 201 on success'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        HttpResponse response = controller.insertCategory(category)

        then:
        response.status == HttpStatus.CREATED
        1 * categoryServiceMock.insertCategory(category)
        0 * _
    }

    void 'test updateCategory - returns 200 on success'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        HttpResponse response = controller.updateCategory('online', category)

        then:
        response.status == HttpStatus.OK
        1 * categoryServiceMock.updateCategory(category) >> true
        0 * _
    }

    void 'test updateCategory - returns 404 when not found'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        HttpResponse response = controller.updateCategory('notfound', category)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * categoryServiceMock.updateCategory(category) >> false
        0 * _
    }

    void 'test deleteByCategoryName - returns 200 when found'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        HttpResponse response = controller.deleteByCategoryName('online')

        then:
        response.status == HttpStatus.OK
        1 * categoryServiceMock.findByCategoryName('online') >> Optional.of(category)
        1 * categoryServiceMock.deleteByCategoryName('online')
        0 * _
    }

    void 'test deleteByCategoryName - returns 400 when not found'() {
        when:
        HttpResponse response = controller.deleteByCategoryName('notfound')

        then:
        response.status == HttpStatus.BAD_REQUEST
        1 * categoryServiceMock.findByCategoryName('notfound') >> Optional.empty()
        0 * _
    }

    void 'test mergeCategories - returns 200 on success'() {
        given:
        Category category = CategoryBuilder.builder().withCategory('new_cat').build()

        when:
        HttpResponse response = controller.mergeCategories('new_cat', 'old_cat')

        then:
        response.status == HttpStatus.OK
        1 * categoryServiceMock.mergeCategories('new_cat', 'old_cat')
        1 * categoryServiceMock.findByCategoryName('new_cat') >> Optional.of(category)
        0 * _
    }

    void 'test mergeCategories - returns 400 on RuntimeException'() {
        when:
        HttpResponse response = controller.mergeCategories('new_cat', 'old_cat')

        then:
        response.status == HttpStatus.BAD_REQUEST
        1 * categoryServiceMock.mergeCategories('new_cat', 'old_cat') >> { throw new RuntimeException('merge failed') }
        0 * _
    }
}
