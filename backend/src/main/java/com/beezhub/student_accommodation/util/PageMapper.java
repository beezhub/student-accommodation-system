package com.beezhub.student_accommodation.util;

import com.beezhub.student_accommodation.model.dto.PageResponse;
import org.springframework.data.domain.Page;

public class PageMapper {
    
    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}
