package com.flashkart.catalog.infra;

import com.flashkart.catalog.domain.Category;
import com.flashkart.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ProductRepository.
 * Tests derived queries, JPQL queries, and native queries.
 */
@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findByActiveTrueAndDeletedAtIsNull_ReturnsActiveProducts() {
        // Given
        Category category = createCategory("Electronics");
        Product product1 = createProduct("Laptop", category, true);
        Product product2 = createProduct("Phone", category, false);
        entityManager.persistAndFlush(product1);
        entityManager.persistAndFlush(product2);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByActiveTrueAndDeletedAtIsNull(pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals("Laptop", result.getContent().get(0).getName());
    }

    @Test
    void findByCategoryIdAndActiveTrueAndDeletedAtIsNull_ReturnsFilteredProducts() {
        // Given
        Category category1 = createCategory("Electronics");
        Category category2 = createCategory("Clothing");
        Product product1 = createProduct("Laptop", category1, true);
        Product product2 = createProduct("Shirt", category2, true);
        entityManager.persistAndFlush(product1);
        entityManager.persistAndFlush(product2);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByCategoryIdAndActiveTrueAndDeletedAtIsNull(
                category1.getId(), pageable
        );

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals("Laptop", result.getContent().get(0).getName());
    }

    @Test
    void findByBrandAndActiveTrueAndDeletedAtIsNull_ReturnsFilteredProducts() {
        // Given
        Category category = createCategory("Electronics");
        Product product1 = createProduct("Laptop", category, true);
        product1.setBrand("Dell");
        Product product2 = createProduct("Phone", category, true);
        product2.setBrand("Apple");
        entityManager.persistAndFlush(product1);
        entityManager.persistAndFlush(product2);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByBrandAndActiveTrueAndDeletedAtIsNull(
                "Dell", pageable
        );

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals("Dell", result.getContent().get(0).getBrand());
    }

    @Test
    void findByNameContainingIgnoreCaseAndActiveTrueAndDeletedAtIsNull_ReturnsMatchingProducts() {
        // Given
        Category category = createCategory("Electronics");
        Product product1 = createProduct("Laptop Dell", category, true);
        Product product2 = createProduct("Phone", category, true);
        entityManager.persistAndFlush(product1);
        entityManager.persistAndFlush(product2);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByNameContainingIgnoreCaseAndActiveTrueAndDeletedAtIsNull(
                "laptop", pageable
        );

        // Then
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getName().toLowerCase().contains("laptop"));
    }

    @Test
    void findActiveProductsWithCategory_UsesJoinFetch_LoadsCategoryEagerly() {
        // Given
        Category category = createCategory("Electronics");
        Product product = createProduct("Laptop", category, true);
        entityManager.persistAndFlush(product);

        // When
        List<Product> result = productRepository.findActiveProductsWithCategory();

        // Then
        assertEquals(1, result.size());
        Product found = result.get(0);
        assertNotNull(found.getCategory());
        assertEquals("Electronics", found.getCategory().getName());
        // Category should be loaded (no lazy loading exception)
        assertDoesNotThrow(() -> found.getCategory().getName());
    }

    @Test
    void findByIdWithCategory_UsesJoinFetch_LoadsCategoryEagerly() {
        // Given
        Category category = createCategory("Electronics");
        Product product = createProduct("Laptop", category, true);
        entityManager.persistAndFlush(product);

        // When
        Optional<Product> result = productRepository.findByIdWithCategory(product.getId());

        // Then
        assertTrue(result.isPresent());
        Product found = result.get();
        assertNotNull(found.getCategory());
        assertEquals("Electronics", found.getCategory().getName());
        // Category should be loaded (no lazy loading exception)
        assertDoesNotThrow(() -> found.getCategory().getName());
    }

    @Test
    void searchProducts_WithFilters_ReturnsFilteredResults() {
        // Given
        Category category = createCategory("Electronics");
        Product product1 = createProduct("Laptop", category, true);
        product1.setBrand("Dell");
        Product product2 = createProduct("Phone", category, true);
        product2.setBrand("Apple");
        entityManager.persistAndFlush(product1);
        entityManager.persistAndFlush(product2);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.searchProducts(
                category.getId(), "Dell", "laptop", pageable
        );

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals("Laptop", result.getContent().get(0).getName());
    }

    @Test
    void fullTextSearch_WithNativeQuery_ReturnsRelevantResults() {
        // Given
        Category category = createCategory("Electronics");
        Product product1 = createProduct("Laptop Computer", category, true);
        Product product2 = createProduct("Mobile Phone", category, true);
        entityManager.persistAndFlush(product1);
        entityManager.persistAndFlush(product2);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        List<Product> result = productRepository.fullTextSearch("laptop", pageable);

        // Then
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(p -> p.getName().toLowerCase().contains("laptop")));
    }

    // Helper methods
    private Category createCategory(String name) {
        Category category = new Category(name, "Description", "image.jpg");
        entityManager.persistAndFlush(category);
        return category;
    }

    private Product createProduct(String name, Category category, boolean active) {
        Product product = new Product(name, "Description", category, "Brand", "image.jpg");
        product.setActive(active);
        return product;
    }
}
