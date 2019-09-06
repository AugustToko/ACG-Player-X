package top.geek_studio.chenlongcould.musicplayer.ui.activities.base;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import android.view.KeyEvent;
import android.view.View;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.chenlongcould.musicplayer.R;

/**
 * AbsBaseActivity
 *
 * 权限, 基础设置
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public abstract class AbsBaseActivity extends AbsThemeActivity {

    /**
     * for {@link #onActivityResult(int, int, Intent)}
     */
    public static final int PERMISSION_REQUEST = 100;

    private boolean hadPermissions;
    private String[] permissions;
    private String permissionDeniedMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置音频流
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        permissions = getPermissionsToRequest();

        hadPermissions = hasPermissions();

        setPermissionDeniedMessage(null);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // 如果没有权限, 获取
        if (!hasPermissions()) {
            requestPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 再次检测权限
        final boolean hasPermissions = hasPermissions();
        if (hasPermissions != hadPermissions) {
            hadPermissions = hasPermissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                onHasPermissionsChanged(hasPermissions);
            }
        }
    }

    /**
     * 权限得到了变化
     *
     * @param hasPermissions 是否拥有权限
     */
    protected void onHasPermissionsChanged(boolean hasPermissions) {
        // implemented by sub classes
    }

    /**
     * 按键事件调度
     */
    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_UP) {
            showOverflowMenu();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * 显示菜单 (浮动菜单)
     * */
    protected void showOverflowMenu() {

    }

    /**
     * 获取所需权限
     *
     * @return 返回权限数组
     * */
    @Nullable
    protected String[] getPermissionsToRequest() {
        return null;
    }

    /**
     * 获取 SnackBar 容器
     *
     * @return view
     * */
    protected View getSnackBarContainer() {
        return getWindow().getDecorView();
    }

    protected void setPermissionDeniedMessage(String message) {
        permissionDeniedMessage = message;
    }

    private String getPermissionDeniedMessage() {
        return permissionDeniedMessage == null ? getString(R.string.permissions_denied) : permissionDeniedMessage;
    }

    /**
     * 请求权限
     * */
    protected void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            requestPermissions(permissions, PERMISSION_REQUEST);
        }
    }

    /**
     * 检测权限
     * */
    protected boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(AbsBaseActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // User has deny from permission dialog
                        Snackbar.make(getSnackBarContainer(), getPermissionDeniedMessage(),
                                Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.action_grant, view -> requestPermissions())
                                .setActionTextColor(ThemeStore.accentColor(this))
                                .show();
                    } else {
                        // User has deny permission and checked never show permission dialog so you can redirect to Application settings page
                        Snackbar.make(getSnackBarContainer(), getPermissionDeniedMessage(),
                                Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.action_settings, view -> {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", AbsBaseActivity.this.getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                })
                                .setActionTextColor(ThemeStore.accentColor(this))
                                .show();
                    }
                    return;
                }
            }
            hadPermissions = true;
            onHasPermissionsChanged(true);
        }
    }
}
