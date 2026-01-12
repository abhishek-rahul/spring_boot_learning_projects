package com.flashkart.catalog.service;

import com.flashkart.catalog.api.dto.ProductResponse;
import com.flashkart.catalog.api.dto.ProductSummaryResponse;
import com.flashkart.catalog.domain.Category;
import com.flashkart.catalog.domain.Price;
import com.flashkart.catalog.domain.Product;
import com.flashkart.catalog.domain.Sku;
import com.flashkart.catalog.infra.*;
import com.flashkart.shared.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductService.
 * Tests cache-aside pattern, N+1 prevention, and repository patterns.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductSummaryRepository productSummaryRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private ProductService productService;

    private UUID productId;
    private UUID categoryId;
    private Product product;
    private Category category;
    private Sku sku;
    private Price price;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = new Category("Electronics", "Electronic products", "image.jpg");
        category = createCategoryWithId(categoryId, category);

        product = new Product("Laptop", "High-performance laptop", category, "Dell", "laptop.jpg");
        product = createProductWithId(productId, product);

        price = new Price(null, new BigDecimal("999.99"), "USD", 1);
        price = createPriceWithId(UUID.randomUUID(), price);
        price.setCurrent(true);

        sku = new Sku("LAPTOP-001", "16GB RAM - 512GB SSD", product, 10);
        sku = createSkuWithId(UUID.randomUUID(), sku);
        sku.setCurrentPrice(price);
    }

    @Test
    void getById_WhenProductExists_ReturnsProductResponse() {
        // Given - cache miss
        when(cacheService.get(anyString(), eq(ProductResponse.class))).thenReturn(Optional.empty());
        when(productRepository.findByIdWithCategory(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByProductIdWithPrice(productId)).thenReturn(List.of(sku));

        // When
        ProductResponse response = productService.getById(productId);

        // Then
        assertNotNull(response);
        assertEquals(productId, response.id());
        assertEquals("Laptop", response.name());
        assertEquals(1, response.skus().size());
        verify(cacheService).put(anyString(), any(ProductResponse.class));
    }

    @Test
    void getById_WhenProductInCache_ReturnsCachedResponse() {
        // Given - cache hit
        ProductResponse cachedResponse = new ProductResponse(
                productId, "Laptop", "Description", "Dell", "image.jpg",
                null, List.of(), true, Instant.now(), Instant.now()
        );
        when(cacheService.get(anyString(), eq(ProductResponse.class))).thenReturn(Optional.of(cachedResponse));

        // When
        ProductResponse response = productService.getById(productId);

        // Then
        assertNotNull(response);
        assertEquals(cachedResponse, response);
        verify(productRepository, never()).findByIdWithCategory(any());
        verify(cacheService, never()).put(anyString(), any());
    }

    @Test
    void getById_WhenProductNotFound_ThrowsNotFoundException() {
        // Given
        when(cacheService.get(anyString(), eq(ProductResponse.class))).thenReturn(Optional.empty());
        when(productRepository.findByIdWithCategory(productId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productService.getById(productId));
        verify(cacheService, never()).put(anyString(), any());
    }

    @Test
    void searchProducts_WithFilters_ReturnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        ProductProjection projection = createMockProjection();
        Page<ProductProjection> projectionPage = new PageImpl<>(List.of(projection), pageable, 1);

        when(productSummaryRepository.findProductSummaries(
                eq(categoryId), eq("Dell"), eq("laptop"), any(Pageable.class)
        )).thenReturn(projectionPage);

        // When
        Page<ProductSummaryResponse> result = productService.searchProducts(
                categoryId, "Dell", "laptop", 0, 20, "createdAt", "DESC"
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(productSummaryRepository).findProductSummaries(
                eq(categoryId), eq("Dell"), eq("laptop"), any(Pageable.class)
        );
    }

    @Test
    void searchProducts_WithNullFilters_ReturnsAllProducts() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        ProductProjection projection = createMockProjection();
        Page<ProductProjection> projectionPage = new PageImpl<>(List.of(projection), pageable, 1);

        when(productSummaryRepository.findProductSummaries(
                isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(projectionPage);

        // When
        Page<ProductSummaryResponse> result = productService.searchProducts(
                null, null, null, 0, 20, "createdAt", "DESC"
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void invalidateProductCache_CallsCacheService() {
        // When
        productService.invalidateProductCache(productId);

        // Then
        verify(cacheService).delete("product:" + productId);
        verify(cacheService).deletePattern("products:*");
    }

    // Helper methods
    private Product createProductWithId(UUID id, Product product) {
        try {
            var idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, id);
            return product;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Category createCategoryWithId(UUID id, Category category) {
        try {
            var idField = Category.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(category, id);
            return category;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Sku createSkuWithId(UUID id, Sku sku) {
        try {
            var idField = Sku.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sku, id);
            return sku;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Price createPriceWithId(UUID id, Price price) {
        try {
            var idField = Price.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(price, id);
            return price;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ProductProjection createMockProjection() {
        return new ProductProjection() {
            @Override
            public UUID getId() {
                return productId;
            }

            @Override
            public String getName() {
                return "Laptop";
            }

            @Override
            public String getDescription() {
                return "Description";
            }

            @Override
            public String getBrand() {
                return "Dell";
            }

            @Override
            public String getImageUrl() {
                return "image.jpg";
            }

            @Override
            public UUID getCategoryId() {
                return categoryId;
            }

            @Override
            public String getCategoryName() {
                return "Electronics";
            }

            @Override
            public BigDecimal getMinPrice() {
                return new BigDecimal("999.99");
            }

            @Override
            public BigDecimal getMaxPrice() {
                return new BigDecimal("999.99");
            }

            @Override
            public boolean isActive() {
                return true;
            }
        };
    }
}
