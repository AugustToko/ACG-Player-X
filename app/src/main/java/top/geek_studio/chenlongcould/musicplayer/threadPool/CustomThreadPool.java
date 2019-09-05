package top.geek_studio.chenlongcould.musicplayer.threadPool;

import androidx.annotation.Nullable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class CustomThreadPool {
	private static final int KEEP_ALIVE = 20;
	private static CustomThreadPool mInstance = null;
	private ThreadPoolExecutor mThreadPoolExec;

	private CustomThreadPool() {
		int coreNum = Runtime.getRuntime().availableProcessors();
		int maxPoolSize = coreNum * 2;
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
		mThreadPoolExec = new ThreadPoolExecutor(coreNum, maxPoolSize, KEEP_ALIVE, TimeUnit.SECONDS, workQueue);
	}

	public static synchronized void post(Runnable runnable) {
		if (mInstance == null) {
			mInstance = new CustomThreadPool();
		}
		mInstance.mThreadPoolExec.execute(runnable);
	}

	public static void removeTask(@Nullable Runnable runnable) {
		if (runnable == null) return;
		mInstance.mThreadPoolExec.remove(runnable);
	}

	public static void finish() {
		if (mInstance != null) {
			mInstance.mThreadPoolExec.shutdown();
			mInstance = null;
		}
	}
}
