package com.studyforge.reliability;

import org.springframework.stereotype.Component;

/**
 * 通用重试器：指数退避，用于 LLM 调用/JSON 校验失败后的重试。
 */
@Component
public class RetryRunner {

    /**
     * @param retries     重试次数（不含首次）
     * @param baseDelayMs 首次退避基数，每次翻倍
     * @param action      可抛出 RuntimeException 的动作
     */
    public <T> T run(int retries, long baseDelayMs, ThrowingSupplier<T> action) {
        int attempt = 0;
        while (true) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                if (attempt >= retries) {
                    throw e;
                }
                long delay = baseDelayMs * (1L << attempt);
                attempt++;
                sleep(delay);
            }
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("重试被中断", ie);
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get();
    }
}
