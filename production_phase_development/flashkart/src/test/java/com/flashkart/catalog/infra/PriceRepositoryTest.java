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
 * Integration tests for PriceRepository.
 * Tests price versioning functionality.
 */
@DataJpaTest
@ActiveProfiles("test")
class PriceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findBySkuIdAndIsCurrentTrueAndDeletedAtIsNull_ReturnsCurrentPrice() {
        // Given
        Category category = createCategory("Electronics");
        Product product = createProduct("Laptop", category);
        Sku sku = createSku("SKU-001", product);
        Price oldPrice = createPrice(sku, new BigDecimal("999.99"), 1, false);
        Price currentPrice = createPrice(sku, new BigDecimal("899.99"), 2, true);
        entityManager.persistAndFlush(sku);

        // When
        Optional<Price> result = priceRepository.findBySkuIdAndIsCurrentTrueAndDeletedAtIsNull(sku.getId());

        // Then
        assertTrue(result.isPresent());
        assertEquals(currentPrice.getId(), result.get().getId());
        assertTrue(result.get().isCurrent());
        assertEquals(2, result.get().getVersion());
    }

    @Test
    void findBySkuIdAndDeletedAtIsNullOrderByVersionDesc_ReturnsPriceHistory() {
        // Given
        Category category = createCategory("Electronics");
        Product product = createProduct("Laptop", category);
        Sku sku = createSku("SKU-001", product);
        Price price1 = createPrice(sku, new BigDecimal("999.99"), 1, false);
        Price price2 = createPrice(sku, new BigDecimal("899.99"), 2, false);
        Price price3 = createPrice(sku, new BigDecimal("799.99"), 3, true);
        entityManager.persistAndFlush(sku);

        // When
        List<Price> result = priceRepository.findBySkuIdAndDeletedAtIsNullOrderByVersionDesc(sku.getId());

        // Then
        assertEquals(3, result.size());
        // Should be ordered by version DESC
        assertEquals(3, result.get(0).getVersion());
        assertEquals(2, result.get(1).getVersion());
        assertEquals(1, result.get(2).getVersion());
    }

    @Test
    void findCurrentPricesBySkuIds_ReturnsBulkPrices() {
        // Given
        Category category = createCategory("Electronics");
        Product product = createProduct("Laptop", category);
        Sku sku1 = createSku("SKU-001", product);
        Sku sku2 = createSku("SKU-002", product);
        Price price1 = createPrice(sku1, new BigDecimal("999.99"), 1, true);
        Price price2 = createPrice(sku2, new BigDecimal("1299.99"), 1, true);
        entityManager.persistAndFlush(sku1);
        entityManager.persistAndFlush(sku2);

        // When
        List<Price> result = priceRepository.findCurrentPricesBySkuIds(
                List.of(sku1.getId(), sku2.getId())
        );

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(Price::isCurrent));
    }

    @Test
    void findLatestVersionBySkuId_ReturnsMaxVersion() {
        // Given
        Category category = createCategory("Electronics");
        Product product = createProduct("Laptop", category);
        Sku sku = createSku("SKU-001", product);
        createPrice(sku, new BigDecimal("999.99"), 1, false);
        createPrice(sku, new BigDecimal("899.99"), 2, false);
        createPrice(sku, new BigDecimal("799.99"), 3, true);
        entityManager.persistAndFlush(sku);

        // When
        Integer latestVersion = priceRepository.findLatestVersionBySkuId(sku.getId());

        // Then
        assertEquals(3, latestVersion);
    }

    @Test
    void markAllAsNotCurrent_UpdatesAllCurrentPrices() {
        // Given
        Category category = createCategory("Electronics");
        Product product = createProduct("Laptop", category);
        Sku sku = createSku("SKU-001", product);
        Price price1 = createPrice(sku, new BigDecimal("999.99"), 1, true);
        Price price2 = createPrice(sku, new BigDecimal("899.99"), 2, true);
        entityManager.persistAndFlush(sku);

        // When
        priceRepository.markAllAsNotCurrent(sku.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        List<Price> prices = priceRepository.findBySkuIdAndDeletedAtIsNullOrderByVersionDesc(sku.getId());
        assertTrue(prices.stream().noneMatch(Price::isCurrent));
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

    private Price createPrice(Sku sku, BigDecimal amount, Integer version, boolean isCurrent) {
        Price price = new Price(sku, amount, "USD", version);
        price.setCurrent(isCurrent);
        entityManager.persistAndFlush(price);
        return price;
    }
}
