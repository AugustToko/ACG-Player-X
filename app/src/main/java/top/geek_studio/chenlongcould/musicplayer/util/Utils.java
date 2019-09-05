package top.geek_studio.chenlongcould.musicplayer.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.internal.NavigationMenuPresenter;
import com.google.android.material.internal.NavigationMenuView;
import com.google.android.material.navigation.NavigationView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("WeakerAccess")
public final class Utils {

	public static final String TAG = "Utils";

	private Utils() {
		throw new AssertionError("private!");
	}

	public static final class Ui {

		private static final String TAG = "Ui";

		private Bitmap view2Bitmap(View v) {
			int w = v.getWidth();
			int h = v.getHeight();
			final Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(bmp);
			//如果不设置颜色，默认是透明背景
			c.drawColor(Color.WHITE);
			v.layout(0, 0, w, h);
			v.draw(c);
			return bmp;
		}

		public static Point getScreenSize(@NonNull Context c) {
			Display display = ((WindowManager) c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			return size;
		}

		// FIXME: 2019/5/24
		public static void releaseImageViewResource(@NonNull Context context, @NonNull ImageView imageView, @Nullable Bitmap replace) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof BitmapDrawable) {
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				Bitmap bitmap = bitmapDrawable.getBitmap();
				if (bitmap != null && !bitmap.isRecycled()) {
					Glide.with(context).load(replace).into(imageView);
					bitmap.recycle();
					Log.d(TAG, "releaseImageViewResource: released!");
				} else {
					Log.d(TAG, "releaseImageViewResource: bitmap is null or bitmap isRecycled.");
				}
			}
		}

		public static Bitmap readBitmapFromFile(String filePath, int width, int height) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filePath, options);
			float srcWidth = options.outWidth;
			float srcHeight = options.outHeight;
			int inSampleSize = 1;

			if (srcWidth > height && srcWidth > width) {
				inSampleSize = (int) (srcWidth / width);
			} else if (srcWidth < height && srcHeight > height) {
				inSampleSize = (int) (srcHeight / height);
			}

			if (inSampleSize <= 0) {
				inSampleSize = 1;
			}
			options.inJustDecodeBounds = false;
			options.inSampleSize = inSampleSize;

			return BitmapFactory.decodeFile(filePath, options);
		}

		public static Bitmap readBitmapFromRes(Context context, @DrawableRes int filePath, int width, int height) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeResource(context.getResources(), filePath, options);
			float srcWidth = options.outWidth;
			float srcHeight = options.outHeight;
			int inSampleSize = 1;

			if (srcWidth > height && srcWidth > width) {
				inSampleSize = (int) (srcWidth / width);
			} else if (srcWidth < height && srcHeight > height) {
				inSampleSize = (int) (srcHeight / height);
			}

			if (inSampleSize <= 0) {
				inSampleSize = 1;
			}
			options.inJustDecodeBounds = false;
			options.inSampleSize = inSampleSize;

			return BitmapFactory.decodeResource(context.getResources(), filePath, options);
		}

		public static Bitmap readBitmapFromArray(byte[] data, int width, int height) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, options);
			float srcWidth = options.outWidth;
			float srcHeight = options.outHeight;
			int inSampleSize = 1;

			if (srcWidth > height && srcWidth > width) {
				inSampleSize = (int) (srcWidth / width);
			} else if (srcWidth < height && srcHeight > height) {
				inSampleSize = (int) (srcHeight / height);
			}

			if (inSampleSize <= 0) {
				inSampleSize = 1;
			}
			options.inJustDecodeBounds = false;
			options.inSampleSize = inSampleSize;

			return BitmapFactory.decodeByteArray(data, 0, data.length, options);
		}

//		/**
//		 * @param context context
//		 * @param aTitle  theTitle
//		 * @return {@link AlertDialog.Builder}
//		 * @deprecated use {@see top.geek_studio.chenlongcould.geeklibrary.DialogUtil#getLoadingDialog(Context, String...) }
//		 */
//		public static androidx.appcompat.app.AlertDialog getLoadingDialog(Context context, String... aTitle) {
//			androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
//			final View loadView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
//			// TODO: 2019/1/7 custom Theme loading animation
//			builder.setView(loadView);
//			builder.setTitle(aTitle.length == 0 ? "Loading..." : aTitle[0]);
//			builder.setCancelable(false);
//			return builder.create();
//		}

		public static void setNavigationMenuLineStyle(NavigationView navigationView, @ColorInt final int color, final int height) {
			try {
				Field fieldByPressenter = navigationView.getClass().getDeclaredField("presenter");
				fieldByPressenter.setAccessible(true);
				NavigationMenuPresenter menuPresenter = (NavigationMenuPresenter) fieldByPressenter.get(navigationView);
				Field fieldByMenuView = menuPresenter.getClass().getDeclaredField("menuView");
				fieldByMenuView.setAccessible(true);
				final NavigationMenuView mMenuView = (NavigationMenuView) fieldByMenuView.get(menuPresenter);

				mMenuView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
					@Override
					public void onChildViewAttachedToWindow(@NonNull View view) {

						RecyclerView.ViewHolder viewHolder = mMenuView.getChildViewHolder(view);
						if (viewHolder != null && "SeparatorViewHolder".equals(viewHolder.getClass().getSimpleName())) {
							if (viewHolder.itemView instanceof FrameLayout) {
								FrameLayout frameLayout = (FrameLayout) viewHolder.itemView;
								View line = frameLayout.getChildAt(0);
								line.setBackgroundColor(color);
								line.getLayoutParams().height = height;
								line.setLayoutParams(line.getLayoutParams());
							}
						}
					}

					@Override
					public void onChildViewDetachedFromWindow(@NonNull View view) {

					}
				});
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		/**
		 * 获取导航栏高度
		 *
		 * @param context context
		 * @return nav height
		 */
		public static int getNavheight(final Context context) {
			Resources resources = context.getResources();
			int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
			int height = resources.getDimensionPixelSize(resourceId);
			Log.v("dbw", "Navi height:" + height);
			return height;
		}

		public static void setStatusBarTextColor(final Activity activity, @ColorInt int color) {
			final View decor = activity.getWindow().getDecorView();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				double lum = ColorUtils.calculateLuminance(color);
				Log.d(TAG, "setStatusBarTextColor: calculateLuminance: " + lum);
				if (lum <= 0.5) {
					decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
				} else {
					decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
				}
			}
		}

		public static AlertDialog createMessageDialog(@NonNull final Activity context, @NonNull final String title, @NonNull final String message, boolean... touch2Dissmiss) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(title);
			builder.setMessage(message);
			builder.setCancelable(touch2Dissmiss.length <= 0 || touch2Dissmiss[0]);
			return builder.create();
		}

		public static void showErrorDialog(@NonNull final Activity context, @NonNull final String title, @NonNull final String message, @NonNull Runnable runnable, int delayTime) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(title);
			builder.setMessage(message);
			builder.setCancelable(true);
			builder.create().show();

			new Handler().postDelayed(runnable, delayTime);
		}

		public static void fastToast(@NonNull final Context context, @NonNull final String content) {
			Toast.makeText(context, content, Toast.LENGTH_SHORT).show();
		}

		/**
		 * 获取图片亮度
		 *
		 * @param bm bitmap
		 */
		public static int getBright(@NonNull Bitmap bm) {
			if (bm.isRecycled()) return 0;

			int width = bm.getWidth() / 4;
			int height = bm.getHeight() / 4;
			int r, g, b;
			int count = 0;
			int bright = 0;
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					count++;
					int localTemp = bm.getPixel(i, j);
					r = (localTemp | 0xff00ffff) >> 16 & 0x00ff;
					g = (localTemp | 0xffff00ff) >> 8 & 0x0000ff;
					b = (localTemp | 0xffffff00) & 0x0000ff;
					bright = (int) (bright + 0.299 * r + 0.587 * g + 0.114 * b);
				}
			}
			return bright / count;
		}

		/**
		 * 判断颜色是不是亮色
		 *
		 * @param color
		 * @return bool
		 */
		public static boolean isColorLight(@ColorInt int color) {
			return ColorUtils.calculateLuminance(color) >= 0.5;
		}

		/**
		 * by kabouzeid
		 */
		@ColorInt
		public static int withAlpha(@ColorInt int baseColor, @FloatRange(from = 0.0D, to = 1.0D) float alpha) {
			int a = Math.min(255, Math.max(0, (int) (alpha * 255.0F))) << 24;
			int rgb = 16777215 & baseColor;
			return a + rgb;
		}
	}

	public static final class M3Utils {
		// TODO: 2018/12/10 read the m3u file
//            new AsyncTask<Void, Void, Void>() {
//
//                @Override
//                protected Void doInBackground(Void... voids) {
//
////                    ArrayList<String> pathList = new ArrayList<>();
////                    ArrayList<String> nameList = new ArrayList<>();
////                    BufferedReader bufr = null;
////                    try {
////                        FileReader fr = new FileReader(file);
////                        bufr = new BufferedReader(fr);
////                        String line;
////
////                        try {
////                            while ((line = bufr.readLine()) != null) {
////                                if (line.contains("#EXTM3U")) continue;
////                                if (line.contains("#EXTINF")) {
////                                    String name = line.substring(line.indexOf(',') + 1);
////                                    nameList.add(name);
////                                    Log.d(TAG, "doInBackground: name: " + name);
////                                }
////                                if (line.contains("/storage/emulated")) {
////                                    pathList.add(line);
////                                    Log.d(TAG, "doInBackground: path: " + line);
////                                }
////                            }
////                        } catch (IOException e) {
////                            e.printStackTrace();
////                        }
////
////                    } catch (FileNotFoundException e) {
////                        e.printStackTrace();
////                    } finally {
////                        if (bufr != null) {
////                            try {
////                                bufr.close();
////                            } catch (IOException e) {
////                                e.printStackTrace();
////                            }
////                        }
////                    }
//                    return null;
//                }
//            }.execute();

	}

	public static final class Res {
		public static String getString(final Context context, @StringRes final int id) {
			return context.getResources().getString(id);
		}
	}

	public static final class IO {

		public static void Unzip(String zipFile, String targetDir) {
			int BUFFER = 4096;          //这里缓冲区我们使用4KB，
			String strEntry;            //保存每个zip的条目名称

			try {
				BufferedOutputStream dest;          //缓冲输出流
				FileInputStream fis = new FileInputStream(zipFile);
				ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
				ZipEntry entry;         //每个zip条目的实例

				while ((entry = zis.getNextEntry()) != null) {

					try {
						Log.i("unzip: ", "=" + entry);

						int count;
						byte[] data = new byte[BUFFER];
						strEntry = entry.getName();

						File entryFile = new File(targetDir + strEntry);
						File entryDir = new File(entryFile.getParent());

						if (!entryDir.exists()) {
							entryDir.mkdirs();
						}

						if (!entry.isDirectory()) {
							FileOutputStream fos = new FileOutputStream(entryFile);

							dest = new BufferedOutputStream(fos, BUFFER);
							while ((count = zis.read(data, 0, BUFFER)) != -1) {
								dest.write(data, 0, count);
							}
							dest.flush();
							dest.close();
						} else {
							entryFile.mkdir();
						}

					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				zis.close();
			} catch (Exception cwj) {
				cwj.printStackTrace();
			}
		}

		@SuppressWarnings("ResultOfMethodCallIgnored")
		public static void delFolder(String folderPath) {
			try {
				delAllFile(folderPath); //删除完里面所有内容
				File myFilePath = new File(folderPath);
				myFilePath.delete(); //删除空文件夹
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@SuppressWarnings({"UnusedReturnValue", "ConstantConditions", "ResultOfMethodCallIgnored"})
		private static boolean delAllFile(String path) {
			boolean flag = false;
			File file = new File(path);
			if (!file.exists()) {
				return flag;
			}
			if (!file.isDirectory()) {
				return flag;
			}
			String[] tempList = file.list();
			File temp = null;
			for (String aTempList : tempList) {
				if (path.endsWith(File.separator)) {
					temp = new File(path + aTempList);
				} else {
					temp = new File(path + File.separator + aTempList);
				}
				if (temp.isFile()) {
					temp.delete();
				}
				if (temp.isDirectory()) {
					delAllFile(path + "/" + aTempList);//先删除文件夹里面的文件
					delFolder(path + "/" + aTempList);//再删除空文件夹
					flag = true;
				}
			}
			return flag;
		}

		/**
		 * 压缩一个文件夹
		 */
		public static void zipDirectory(String path, String savePath) throws IOException {
			File file = new File(path);
			File zipFile = new File(savePath);
			Log.d(TAG, "zipDirectory: " + zipFile.getAbsolutePath());
			zipFile.createNewFile();
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
			zip(zos, file, file.getName());
			zos.flush();
			zos.close();
		}

		/**
		 * @param zos  压缩输出流
		 * @param file 当前需要压缩的文件
		 * @param path 当前文件相对于压缩文件夹的路径
		 */
		private static void zip(ZipOutputStream zos, File file, String path) throws IOException {
			// 首先判断是文件，还是文件夹，文件直接写入目录进入点，文件夹则遍历
			if (file.isDirectory()) {
				ZipEntry entry = new ZipEntry(path + File.separator);// 文件夹的目录进入点必须以名称分隔符结尾
				zos.putNextEntry(entry);
				File[] files = file.listFiles();
				for (File x : files) {
					zip(zos, x, path + File.separator + x.getName());
				}
			} else {
				FileInputStream fis = new FileInputStream(file);// 目录进入点的名字是文件在压缩文件中的路径
				ZipEntry entry = new ZipEntry(path);
				zos.putNextEntry(entry);// 建立一个目录进入点

				int len = 0;
				byte[] buf = new byte[1024];
				while ((len = fis.read(buf)) != -1) {
					zos.write(buf, 0, len);
				}
				zos.flush();
				fis.close();
				zos.closeEntry();// 关闭当前目录进入点，将输入流移动下一个目录进入点
			}
		}
	}
}
