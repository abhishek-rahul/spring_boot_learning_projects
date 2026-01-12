package com.flashkart.catalog.infra;

import com.flashkart.catalog.domain.Category;
import com.flashkart.catalog.domain.Price;
import com.flashkart.catalog.domain.Product;
import com.flashkart.catalog.domain.Sku;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SkuRepository.
 * Tests N+1 prevention with JOIN FETCH.
 */
@DataJpaTest
@ActiveProfiles("test")
class SkuRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findBySkuCode_ReturnsSku() {
        // Given
        Category category = createCategory("Electronics");
        Product product = createProduct("Laptop", category);
        Sku sku = createSku("SKU-001", product);
        entityManager.persistAndFlush(sku);

        // When
        Optional<Sku> result = skuRepository.findBySkuCode("SKU-001");

        // Then
        assertTrue(result.isPresent());
        assertEquals("SKU-001", result.get().getSkuCode());
    }

    @Test
    void findByProductIdAndActiveTrueAndDeletedAtIsNull_ReturnsActiveSkus() {
        // Given
        Category category = createCategory("Electronics");
        Product product = createProduct("Laptop", category);
        Sku sku1 = createSku("SKU-001", product);
        sku1.setActive(true);
        Sku sku2 = createSku("SKU-002", product);
        sku2.setActive(false);
        entityManager.persistAndFlush(sku1);
        entityManager.persistAndFlush(sku2);

        // When
        List<Sku> result = skuRepository.findByProductIdAndActiveTrueAndDeletedAtIsNull(product.getId());

        // Then
        assertEquals(1, result.size());
        assertEquals("SKU-001", result.get(0).getSkuCode());
    }

    @Test
    void findByIdWithRelations_UsesJoinFetch_LoadsAllRelationsEagerly() {
        // Given
        Category category = createCategory("Electronics");
        Product product = createProduct("Laptop", category);
        Sku sku = createSku("SKU-001", product);
        Price price = createPrice(sku, new BigDecimal("999.99"));
        sku.setCurrentPrice(price);
        entityManager.persistAndFlush(sku);

        // When
        Optional<Sku> result = skuRepository.findByIdWithRelations(sku.getId());

        // Then
        assertTrue(result.isPresent());
        Sku found = result.get();
        
        // All relations should be loaded (no lazy loading exception)
        assertDoesNotThrow(() -> found.getProduct().getName());
        assertDoesNotThrow(() -> found.getProduct().getCategory().getName());
        assertDoesNotThrow(() -> found.getCurrentPrice().getAmount());
        
        assertEquals("Laptop", found.getProduct().getName());
        assertEquals("Electronics", found.getProduct().getCategory().getName());
        assertEquals(new BigDecimal("999.99"), found.getCurrentPrice().getAmount());
    }

    @Test
    void findByProductIdWithPrice_UsesJoinFetch_LoadsPriceEagerly() {
        // Given
        Category category = createCategory("Electronics");
        Product product = createProduct("Laptop", category);
        Sku sku1 = createSku("SKU-001", product);
        Price price1 = createPrice(sku1, new BigDecimal("999.99"));
        sku1.setCurrentPrice(price1);
        Sku sku2 = createSku("SKU-002", product);
        Price price2 = createPrice(sku2, new BigDecimal("1299.99"));
        sku2.setCurrentPrice(price2);
        entityManager.persistAndFlush(sku1);
        entityManager.persistAndFlush(sku2);

        // When
        List<Sku> result = skuRepository.findByProductIdWithPrice(product.getId());

        // Then
        assertEquals(2, result.size());
        // All prices should be loaded (no lazy loading exception)
        result.forEach(sku -> {
            assertDoesNotThrow(() -> sku.getCurrentPrice().getAmount());
            assertNotNull(sku.getCurrentPrice());
        });
    }

    // Helper methods
    private Category createCategory(String name) {
        Category category = new Category(name, "Description", "image.jpg");
        entityManager.persistAndFlush(category);
        return category;
    }

    private Product createProduct(String name, Category category) {
        Product product = new Product(name, "Description", category, "Brand", "image.jpg");
        entityManager.persistAndFlush(product);
        return product;
    }

    private Sku createSku(String skuCode, Product product) {
        Sku sku = new Sku(skuCode, "Variant", product, 10);
        entityManager.persistAndFlush(sku);
        return sku;
    }

    private Price createPrice(Sku sku, BigDecimal amount) {
        Price price = new Price(sku, amount, "USD", 1);
        price.setCurrent(true);
        entityManager.persistAndFlush(price);
        return price;
    }
}
