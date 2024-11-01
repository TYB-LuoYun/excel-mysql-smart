package top.anets.support.springThreadpool;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;

/**
 * @author ftm
 * @date 2024-03-07 16:39
 */
@Slf4j
@AllArgsConstructor
public class MyThreadFactory implements ThreadFactory {

    private final ThreadFactory factory;

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = factory.newThread(r);
        thread.setUncaughtExceptionHandler(GlobalUncaughtExceptionHandler.getInstance());
        return thread;
    }
}

