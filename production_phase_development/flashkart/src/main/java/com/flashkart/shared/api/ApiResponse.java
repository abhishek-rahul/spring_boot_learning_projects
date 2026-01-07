package com.flashkart.shared.api;

import java.time.Instant;

public class ApiResponse<T> {
    public boolean success;
    public String apiVersion;
    public String requestId;
    public Instant timestamp;
    public T data;
    public PaginationMeta pagination;

    public static <T> ApiResponse<T> ok(String apiVersion, String requestId, T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.apiVersion = apiVersion;
        r.requestId = requestId;
        r.timestamp = Instant.now();
        r.data = data;
        return r;
    }

    public ApiResponse<T> withPagination(PaginationMeta p) {
        this.pagination = p;
        return this;
    }
}
