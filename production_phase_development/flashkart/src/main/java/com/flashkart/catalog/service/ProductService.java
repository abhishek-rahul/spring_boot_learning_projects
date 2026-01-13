package com.flashkart.catalog.service;

import com.flashkart.catalog.api.dto.*;
import com.flashkart.catalog.domain.Price;
import com.flashkart.catalog.domain.Product;
import com.flashkart.catalog.domain.Sku;
import com.flashkart.catalog.infra.*;
import com.flashkart.shared.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Product service with cache-aside pattern for product details.
 * Implements caching strategy to improve performance.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private static final String CACHE_KEY_PRODUCT = "product:";
    private static final String CACHE_KEY_PRODUCT_LIST = "products:";

    private final ProductRepository productRepository;
    private final ProductSummaryRepository productSummaryRepository;
    private final SkuRepository skuRepository;
    private final CacheService cacheService;

    public ProductService(
            ProductRepository productRepository,
            ProductSummaryRepository productSummaryRepository,
            SkuRepository skuRepository,
            CacheService cacheService) {
        this.productRepository = productRepository;
        this.productSummaryRepository = productSummaryRepository;
        this.skuRepository = skuRepository;
        this.cacheService = cacheService;
    }

    /**
     * Get product by ID with cache-aside pattern.
     * 1. Check cache first
     * 2. If not found, query database
     * 3. Store in cache
     */
    public ProductResponse getById(UUID productId) {
        String cacheKey = CACHE_KEY_PRODUCT + productId;

        // Try cache first (cache-aside pattern)
        var cached = cacheService.get(cacheKey, ProductResponse.class);
        if (cached.isPresent()) {
            logger.debug("Product {} found in cache", productId);
            return cached.get();
        }

        // Cache miss - query database
        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        // Load SKUs with prices (avoiding N+1 problem)
        List<Sku> skus = skuRepository.findByProductIdWithPrice(productId);

        // Convert to response
        ProductResponse response = toProductResponse(product, skus);

        // Store in cache
        cacheService.put(cacheKey, response);

        return response;
    }

    /**
     * Search products with pagination, sorting, and filtering.
     * Uses projection for optimized queries.
     */
    public Page<ProductSummaryResponse> searchProducts(
            UUID categoryId,
            String brand,
            String searchTerm,
            int page,
            int size,
            String sortBy,
            String sortDirection) {

        // Build pageable
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        // Query using projection for performance
        Page<ProductProjection> projections = productSummaryRepository.findProductSummaries(
                categoryId,
                brand,
                searchTerm,
                pageable
        );

        // Convert projections to DTOs
        List<ProductSummaryResponse> content = projections.getContent().stream()
                .map(this::toProductSummaryResponse)
                .toList();

        return new PageImpl<>(content, pageable, projections.getTotalElements());
    }

    /**
     * Get all active products with pagination.
     */
    public Page<ProductSummaryResponse> getAllProducts(int page, int size, String sortBy, String sortDirection) {
        return searchProducts(null, null, null, page, size, sortBy, sortDirection);
    }

    /**
     * Invalidate product cache (call after updates/deletes).
     */
    public void invalidateProductCache(UUID productId) {
        cacheService.delete(CACHE_KEY_PRODUCT + productId);
        cacheService.deletePattern(CACHE_KEY_PRODUCT_LIST + "*");
        logger.debug("Invalidated cache for product: {}", productId);
    }

    // ========== Private Helper Methods ==========

    private ProductResponse toProductResponse(Product product, List<Sku> skus) {
        CategoryResponse categoryResponse = null;
        if (product.getCategory() != null) {
            categoryResponse = new CategoryResponse(
                    product.getCategory().getId(),
                    product.getCategory().getName(),
                    product.getCategory().getDescription(),
                    product.getCategory().getImageUrl(),
                    product.getCategory().isActive(),
                    product.getCategory().getCreatedAt(),
                    product.getCategory().getUpdatedAt()
            );
        }

        List<SkuResponse> skuResponses = skus.stream()
                .map(this::toSkuResponse)
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getImageUrl(),
                categoryResponse,
                skuResponses,
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private SkuResponse toSkuResponse(Sku sku) {
        PriceResponse priceResponse = null;
        if (sku.getCurrentPrice() != null) {
            Price price = sku.getCurrentPrice();
            priceResponse = new PriceResponse(
                    price.getId(),
                    price.getAmount(),
                    price.getCurrency(),
                    price.getVersion(),
                    price.isCurrent(),
                    price.getCreatedAt()
            );
        }

        return new SkuResponse(
                sku.getId(),
                sku.getSkuCode(),
                sku.getVariantName(),
                sku.getStockQuantity(),
                sku.getReservedQuantity(),
                sku.getAvailableQuantity(),
                priceResponse,
                sku.isActive(),
                sku.getCreatedAt()
        );
    }

    private ProductSummaryResponse toProductSummaryResponse(ProductProjection projection) {
        return new ProductSummaryResponse(
                projection.getId(),
                projection.getName(),
                projection.getDescription(),
                projection.getBrand(),
                projection.getImageUrl(),
                projection.getCategoryId(),
                projection.getCategoryName(),
                projection.getMinPrice(),
                projection.getMaxPrice(),
                projection.isActive()
        );
    }
}
