/**
 * You can modify and use this source freely
 * only for the development of application related Live2D.
 * <p>
 * (c) Live2D Inc. All rights reserved.
 */
package top.geek_studio.chenlongcould.musicplayer.live2d.framework.jp.live2d.framework;

public class Live2DFramework {
	private static IPlatformManager platformManager;

	public Live2DFramework() {

	}

	public static IPlatformManager getPlatformManager() {
		return platformManager;
	}

	public static void setPlatformManager(IPlatformManager platformManager) {
		Live2DFramework.platformManager = platformManager;
	}


}
