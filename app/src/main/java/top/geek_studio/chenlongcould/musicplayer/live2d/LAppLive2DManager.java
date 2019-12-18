/*
 * You can modify and use this source freely
 * only for the development of application related Live2D.
 * <p>
 * (c) Live2D Inc. All rights reserved.
 */
package top.geek_studio.chenlongcould.musicplayer.live2d;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.util.Log;

import top.geek_studio.chenlongcould.musicplayer.live2d.framework.jp.live2d.framework.L2DViewMatrix;
import top.geek_studio.chenlongcould.musicplayer.live2d.framework.jp.live2d.framework.Live2DFramework;
import top.geek_studio.chenlongcould.musicplayer.live2d.utils.android.FileManager;
import top.geek_studio.chenlongcould.musicplayer.live2d.utils.android.SoundManager;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;

import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nullable;
import javax.microedition.khronos.opengles.GL10;

import jp.live2d.Live2D;

public class LAppLive2DManager {

    private static final String TAG = "SampleLive2DManager";

    private LAppView view;

    private ArrayList<LAppModel> models;

    private int modelCount = -1;
    private boolean reloadFlg;

    public LAppLive2DManager() {
        Live2D.init();
        Live2DFramework.setPlatformManager(new PlatformManager());

        models = new ArrayList<>();
    }

    @Nullable
    public LAppView getView() {
        return view;
    }

    /**
     * 释放资源
     */
    public void release() {
        onPause();
        FileManager.clear();
        SoundManager.release();
        view.setOnClickListener(null);
        view = null;
        releaseModel();
    }

    /**
     * 释放模型
     * */
    private void releaseModel() {
        for (int i = 0; i < models.size(); i++) {
            models.get(i).release();
        }

        models.clear();
    }

    public void update(GL10 gl) {
        view.update();
        if (reloadFlg) {

            reloadFlg = false;

            int no = modelCount % 3;

            try {
                switch (no) {
                    case 0:
                        releaseModel();
                        models.add(new LAppModel());
                        models.get(0).load(gl, LAppDefine.MODEL_HARU);
                        models.get(0).feedIn();
                        break;
                    case 1:
                        releaseModel();

                        models.add(new LAppModel());
                        models.get(0).load(gl, LAppDefine.MODEL_TERISA);
                        models.get(0).feedIn();
                        break;
//					case 2:
//						releaseModel();
//
//						models.add(new LAppModel());
//						models.get(0).load(gl, LAppDefine.MODEL_WANKO);
//						models.get(0).feedIn();
//						break;
//                    case 2:
//                        releaseModel();
//
//                        models.add(new LAppModel());
//                        models.get(0).load(gl, LAppDefine.MODEL_HARU_A);
//                        models.get(0).feedIn();
//
//                        models.add(new LAppModel());
//                        models.get(1).load(gl, LAppDefine.MODEL_HARU_B);
//                        models.get(1).feedIn();
//                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load." + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    public LAppModel getModel(int no) {
        if (no >= models.size()) return null;
        return models.get(no);
    }

    public int getModelNum() {
        return models.size();
    }

    /**
     * @param clearBackground 背景是否透明
     */
    public LAppView createView(Activity act, boolean... clearBackground) {
        view = new LAppView(act);

        if (clearBackground.length > 0 && clearBackground[0]) {
            view.setZOrderOnTop(true);
            view.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            view.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }

        view.setLive2DManager(this);
        CustomThreadPool.post(() -> view.startAccel(act));
        return view;
    }

    public void onResume() {
        if (LAppDefine.DEBUG_LOG) Log.d(TAG, "onResume");
        view.onResume();
    }

    public void onPause() {
        if (LAppDefine.DEBUG_LOG) Log.d(TAG, "onPause");
        view.onPause();
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (LAppDefine.DEBUG_LOG) Log.d(TAG, "onSurfaceChanged " + width + " " + height);
        view.setupView(width, height);

        if (getModelNum() == 0) {
            changeModel();
        }
    }

    /**
     * 更换模型
     */
    public void changeModel() {
        reloadFlg = true;
        modelCount++;
    }

    public boolean tapEvent(float x, float y) {
        if (LAppDefine.DEBUG_LOG) Log.d(TAG, "tapEvent view x:" + x + " y:" + y);

        for (int i = 0; i < models.size(); i++) {
            if (models.get(i).hitTest(LAppDefine.HIT_AREA_HEAD, x, y)) {

                if (LAppDefine.DEBUG_LOG) Log.d(TAG, "Tap face.");
                models.get(i).setRandomExpression();
            } else if (models.get(i).hitTest(LAppDefine.HIT_AREA_BODY, x, y)) {
                if (LAppDefine.DEBUG_LOG) Log.d(TAG, "Tap body.");
                models.get(i).startRandomMotion(LAppDefine.MOTION_GROUP_TAP_BODY, LAppDefine.PRIORITY_NORMAL);
            }
        }
        return true;
    }


    public void flickEvent(float x, float y) {
        if (LAppDefine.DEBUG_LOG) Log.d(TAG, "flick x:" + x + " y:" + y);

        for (int i = 0; i < models.size(); i++) {
            if (models.get(i).hitTest(LAppDefine.HIT_AREA_HEAD, x, y)) {
                if (LAppDefine.DEBUG_LOG) Log.d(TAG, "Flick head.");
                models.get(i).startRandomMotion(LAppDefine.MOTION_GROUP_FLICK_HEAD, LAppDefine.PRIORITY_NORMAL);
            }
        }
    }


    public void maxScaleEvent() {
        if (LAppDefine.DEBUG_LOG) Log.d(TAG, "Max scale event.");

        for (int i = 0; i < models.size(); i++) {
            models.get(i).startRandomMotion(LAppDefine.MOTION_GROUP_PINCH_IN, LAppDefine.PRIORITY_NORMAL);
        }
    }


    public void minScaleEvent() {
        if (LAppDefine.DEBUG_LOG) Log.d(TAG, "Min scale event.");

        for (int i = 0; i < models.size(); i++) {
            models.get(i).startRandomMotion(LAppDefine.MOTION_GROUP_PINCH_OUT, LAppDefine.PRIORITY_NORMAL);
        }
    }


    public void shakeEvent() {
        if (LAppDefine.DEBUG_LOG) Log.d(TAG, "Shake event.");

        for (int i = 0; i < models.size(); i++) {
            models.get(i).startRandomMotion(LAppDefine.MOTION_GROUP_SHAKE, LAppDefine.PRIORITY_FORCE);
        }
    }


    public void setAccel(float x, float y, float z) {
        for (int i = 0; i < models.size(); i++) {
            models.get(i).setAccel(x, y, z);
        }
    }


    public void setDrag(float x, float y) {
        for (int i = 0; i < models.size(); i++) {
            models.get(i).setDrag(x, y);
        }
    }


    public L2DViewMatrix getViewMatrix() {
        return view.getViewMatrix();
    }
}
