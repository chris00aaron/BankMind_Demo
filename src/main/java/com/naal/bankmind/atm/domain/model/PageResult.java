package com.naal.bankmind.atm.domain.model;

import java.util.List;

record PageMetadata(
    long totalElements,
    long totalPages,
    long size,
    long number) {
}

public record PageResult<T>(
    List<T> content, 
    PageMetadata page
) {

    public static <T> PageResult<T> of(List<T> content, long totalElements, long totalPages, long size, long number) {
        return new PageResult<>(content, new PageMetadata(totalElements, totalPages, size, number));
    }

    public static <T> PageResult<T> of(List<T> content, PageMetadata page) {
        return new PageResult<>(content, page);
    }
}


