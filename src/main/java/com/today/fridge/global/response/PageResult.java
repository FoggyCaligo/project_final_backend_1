package com.today.fridge.global.response;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        PageResponse pageInfo
) {}