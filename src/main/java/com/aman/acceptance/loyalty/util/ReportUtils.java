package com.aman.acceptance.loyalty.util;

import java.util.function.Function;

public final class ReportUtils {

    private ReportUtils() {
    }

    public static <T> Long valueOrZero(T result, Function<T, Long> getter) {
        return result == null ? 0L : getter.apply(result);
    }
}
