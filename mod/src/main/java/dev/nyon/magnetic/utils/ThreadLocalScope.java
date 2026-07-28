package dev.nyon.magnetic.utils;

import java.util.function.Supplier;

public final class ThreadLocalScope {

    private ThreadLocalScope() {
    }

    public static <T, R> R call(ThreadLocal<T> local, T value, Supplier<R> action) {
        T previous = local.get();
        local.set(value);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                local.remove();
            } else {
                local.set(previous);
            }
        }
    }

    public static <T> void run(ThreadLocal<T> local, T value, Runnable action) {
        call(local, value, () -> {
            action.run();
            return null;
        });
    }
}
