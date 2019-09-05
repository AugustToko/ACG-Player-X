/**
 * You can modify and use this source freely
 * only for the development of application related Live2D.
 * <p>
 * (c) Live2D Inc. All rights reserved.
 */
package top.geek_studio.chenlongcould.musicplayer.live2d;

import android.util.Log;

import androidx.annotation.Nullable;

import top.geek_studio.chenlongcould.musicplayer.live2d.framework.jp.live2d.framework.IPlatformManager;
import top.geek_studio.chenlongcould.musicplayer.live2d.utils.android.FileManager;
import top.geek_studio.chenlongcould.musicplayer.live2d.utils.android.LoadUtil;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.opengles.GL10;

import jp.live2d.ALive2DModel;
import jp.live2d.android.Live2DModelAndroid;

public class PlatformManager implements IPlatformManager {
	static public final String TAG = "Live2D App";

	private GL10 gl;

	@Nullable
	public byte[] loadBytes(String path) {
		byte[] ret = null;
		try {
			InputStream in = FileManager.open(path);
			if (in == null) return null;
			ret = new byte[in.available()];
			in.read(ret, 0, ret.length);

			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ret;
	}

	@Nullable
	public String loadString(String path) {
		String ret = null;
		try {
			InputStream in = FileManager.open(path);
			if (in == null) return null;

			ret = in.toString();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ret;
	}

	public void loadTexture(ALive2DModel model, int no, String path) {
		try {
			InputStream in = FileManager.open(path);
			if (in == null) return;

			boolean mipmap = true;


			int glTexNo = LoadUtil.loadTexture(gl, in, mipmap);
			((Live2DModelAndroid) model).setTexture(no, glTexNo);

			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void log(String txt) {
		Log.i(TAG, txt);
	}

	public void setGL(GL10 gl) {
		this.gl = gl;
	}

	public ALive2DModel loadLive2DModel(String path) {
		ALive2DModel model = Live2DModelAndroid.loadModel(loadBytes(path));
		return model;
	}


}
