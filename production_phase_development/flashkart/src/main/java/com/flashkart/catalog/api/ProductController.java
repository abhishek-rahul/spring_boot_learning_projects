package com.flashkart.catalog.api;

import com.flashkart.catalog.api.dto.ProductResponse;
import com.flashkart.catalog.api.dto.ProductSummaryResponse;
import com.flashkart.catalog.service.ProductService;
import com.flashkart.shared.api.ApiResponse;
import com.flashkart.shared.api.PaginationMeta;
import com.flashkart.shared.observability.CorrelationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Product controller with pagination, sorting, and filtering support.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product catalog APIs")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Returns detailed product information including SKUs and prices")
    public ApiResponse<ProductResponse> getProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id) {
        ProductResponse product = productService.getById(id);
        String requestId = MDC.get(CorrelationId.MDC_KEY);
        return ApiResponse.ok("v1", requestId, product);
    }

    @GetMapping
    @Operation(
            summary = "Search products",
            description = "Search and filter products with pagination, sorting, and filtering. " +
                    "Supports filtering by category, brand, and search term. " +
                    "Supports sorting by any product field (default: createdAt DESC)")
    public ApiResponse<Page<ProductSummaryResponse>> searchProducts(
            @Parameter(description = "Category ID filter")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Brand filter")
            @RequestParam(required = false) String brand,
            @Parameter(description = "Search term (searches in product name)")
            @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Page<ProductSummaryResponse> result = productService.searchProducts(
                categoryId,
                brand,
                searchTerm,
                page,
                size,
                sortBy,
                sortDirection
        );

        String requestId = MDC.get(CorrelationId.MDC_KEY);
        PaginationMeta pagination = new PaginationMeta(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );

        return ApiResponse.ok("v1", requestId, result)
                .withPagination(pagination);
    }
}
