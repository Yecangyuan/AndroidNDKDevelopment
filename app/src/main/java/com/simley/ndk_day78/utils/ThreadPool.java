package com.simley.ndk_day78.utils;

import com.simley.ndk_day78.R;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class ThreadPool {

    // CPU数量，可能不太准确
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // 核心线程数
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    // 非核心线程数
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    // 非核心线程数的存活时间
    private static final long KEEP_ALIVE_TIME = 30;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    // 对于中小型应用可以设置为64，对于大型应用可以设置为128
    // 待处理的任务将会被加入到该队列中
    private static final BlockingQueue<Runnable> sBlockingQueue = new LinkedBlockingQueue<>(64);
    private static final AtomicLong seq = new AtomicLong();
    private static final ThreadPoolExecutor executor;

    private static ThreadPool mINSTANCE = null;

    // 初始化线程池相关配置
    static {
        executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                sBlockingQueue,
                new MyThreadFactory()
        );
        executor.allowCoreThreadTimeOut(true);
    }


    /**
     * 执行任务
     *
     * @param r
     */
    public void execute(Runnable r) {
        if (r == null) return;
        executor.execute(r);
    }

    /**
     * 移除任务
     *
     * @param r
     */
    public void remove(Runnable r) {
        if (r == null) return;
        executor.remove(r);
    }

    public static ThreadPool getInstance() {
        if (mINSTANCE == null) {
            synchronized (ThreadPool.class) {
                if (mINSTANCE == null) {
                    mINSTANCE = new ThreadPool();
                }
            }
        }
        return mINSTANCE;
    }

    static class MyThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(null, r, "Thread-" + seq.getAndIncrement(), -(512 * 1024));
        }
    }


}
