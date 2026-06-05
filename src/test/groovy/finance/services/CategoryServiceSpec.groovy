package finance.services

import finance.domain.Category
import finance.helpers.CategoryBuilder

import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class CategoryServiceSpec extends BaseServiceSpec {

    void setup() {
    }

    void 'test - insert category'() {
        given:
        Category category = CategoryBuilder.builder().build()
        Set<ConstraintViolation<Category>> constraintViolations = validator.validate(category)

        when:
        categoryService.insertCategory(category)

        then:
        1 * validatorMock.validate(category) >> constraintViolations
        1 * categoryRepositoryMock.saveAndFlush(category)
        0 * _
    }

    void 'test - insert category empty categoryName'() {
        given:
        Category category = CategoryBuilder.builder().withCategory('').build()
        Set<ConstraintViolation<Category>> constraintViolations = validator.validate(category)

        when:
        categoryService.insertCategory(category)

        then:
        constraintViolations.size() == 1
        thrown(ValidationException)
        1 * validatorMock.validate(category) >> constraintViolations
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test - delete category'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        categoryService.deleteByCategoryName(category.categoryName)

        then:
        1 * categoryRepositoryMock.deleteByCategoryName(category.categoryName)
        0 * _
    }

    void 'test - findByCategoryName - found'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        Optional<Category> result = categoryService.findByCategoryName(category.categoryName)

        then:
        result.isPresent()
        1 * categoryRepositoryMock.findByCategoryName(category.categoryName) >> Optional.of(category)
        0 * _
    }

    void 'test - findByCategoryName - not found'() {
        when:
        Optional<Category> result = categoryService.findByCategoryName('nonexistent')

        then:
        !result.isPresent()
        1 * categoryRepositoryMock.findByCategoryName('nonexistent') >> Optional.empty()
        0 * _
    }

    void 'test - findByOwnerAndCategoryName - found'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        Optional<Category> result = categoryService.findByOwnerAndCategoryName('brian', category.categoryName)

        then:
        result.isPresent()
        1 * categoryRepositoryMock.findByOwnerAndCategoryName('brian', category.categoryName) >> Optional.of(category)
        0 * _
    }

    void 'test - fetchAllActiveCategories - empty list'() {
        when:
        List<Category> results = categoryService.fetchAllActiveCategories()

        then:
        results.isEmpty()
        1 * categoryRepositoryMock.findByActiveStatusOrderByCategoryName(true) >> []
        0 * _
    }

    void 'test - fetchAllActiveCategories - with categories'() {
        given:
        Category category = CategoryBuilder.builder().build()
        List<Object[]> countRows = [['foo', 3L] as Object[]]

        when:
        List<Category> results = categoryService.fetchAllActiveCategories()

        then:
        results.size() == 1
        results[0].categoryCount == 3L
        1 * categoryRepositoryMock.findByActiveStatusOrderByCategoryName(true) >> [category]
        1 * transactionRepositoryMock.countByCategoryNameIn(['foo']) >> countRows
        0 * _
    }

    void 'test - updateCategory - found and updated'() {
        given:
        Category existing = CategoryBuilder.builder().withActiveStatus(true).build()
        Category updated = CategoryBuilder.builder().withActiveStatus(false).build()

        when:
        Boolean result = categoryService.updateCategory(updated)

        then:
        result
        1 * categoryRepositoryMock.findByCategoryName(updated.categoryName) >> Optional.of(existing)
        1 * categoryRepositoryMock.saveAndFlush(existing)
        0 * _
    }

    void 'test - updateCategory - not found returns false'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        Boolean result = categoryService.updateCategory(category)

        then:
        !result
        1 * categoryRepositoryMock.findByCategoryName(category.categoryName) >> Optional.empty()
        0 * _
    }

    void 'test - mergeCategories - success'() {
        given:
        Category newCat = CategoryBuilder.builder().withCategory('new_cat').build()
        Category oldCat = CategoryBuilder.builder().withCategory('old_cat').build()

        when:
        Category result = categoryService.mergeCategories('new_cat', 'old_cat')

        then:
        result.categoryName == 'new_cat'
        1 * categoryRepositoryMock.findByCategoryName('new_cat') >> Optional.of(newCat)
        1 * categoryRepositoryMock.findByCategoryName('old_cat') >> Optional.of(oldCat)
        1 * transactionRepositoryMock.bulkUpdateCategory('old_cat', 'new_cat') >> 5
        1 * categoryRepositoryMock.saveAndFlush(oldCat)
        0 * _
    }

    void 'test - mergeCategories - new category not found throws RuntimeException'() {
        when:
        categoryService.mergeCategories('nonexistent', 'old_cat')

        then:
        thrown(RuntimeException)
        1 * categoryRepositoryMock.findByCategoryName('nonexistent') >> Optional.empty()
        1 * categoryRepositoryMock.findByCategoryName('old_cat') >> Optional.empty()
        0 * _
    }

    void 'test - mergeCategories - old category not found throws RuntimeException'() {
        given:
        Category newCat = CategoryBuilder.builder().withCategory('new_cat').build()

        when:
        categoryService.mergeCategories('new_cat', 'nonexistent')

        then:
        thrown(RuntimeException)
        1 * categoryRepositoryMock.findByCategoryName('new_cat') >> Optional.of(newCat)
        1 * categoryRepositoryMock.findByCategoryName('nonexistent') >> Optional.empty()
        0 * _
    }
}
