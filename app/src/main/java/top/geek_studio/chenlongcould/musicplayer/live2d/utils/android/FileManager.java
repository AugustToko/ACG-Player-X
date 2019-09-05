package top.geek_studio.chenlongcould.musicplayer.live2d.utils.android;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * You can modify and use this source freely
 * only for the development of application related Live2D.
 * <p>
 * (c) Live2D Inc. All rights reserved.
 */
public class FileManager {

	private static WeakReference<Context> context;

	public static void init(Context c) {
		if (context != null && context.get() != null) return;
		context = new WeakReference<>(c);
	}

	public static boolean exists_resource(String path) {
		if (context == null) return false;
		try {
			InputStream afd = context.get().getAssets().open(path);
			afd.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Nullable
	private static InputStream open_resource(String path) throws IOException {
		if (context == null) return null;
		return context.get().getAssets().open(path);
	}

	private static boolean exists_cache(String path) {
		if (context == null) return false;
		File f = new File(context.get().getCacheDir(), path);
		return f.exists();
	}

	@Nullable
	private static InputStream open_cache(String path) throws FileNotFoundException {
		if (context == null) return null;
		File f = new File(context.get().getCacheDir(), path);
		return new FileInputStream(f);
	}

	@Nullable
	private static InputStream open(String path, boolean isCache) throws IOException {
		if (isCache) {
			return open_cache(path);
		} else {
			return open_resource(path);
		}

	}

	@Nullable
	public static InputStream open(String path) throws IOException {
		return open(path, false);
	}

	@Nullable
	static AssetFileDescriptor openFd(String path) throws IOException {
		if (context == null) return null;
		return context.get().getAssets().openFd(path);
	}

	public static void clear() {
		context.clear();
	}
}
