package top.geek_studio.chenlongcould.musicplayer.ui.activities.base;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.LayoutRes;
import androidx.fragment.app.Fragment;

import com.github.mmin18.widget.RealtimeBlurView;
import com.kabouzeid.chenlongcould.musicplayer.R;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.player.AbsPlayerFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.player.MiniPlayerFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.player.NowPlayingScreen;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.player.card.CardPlayerFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.player.flat.FlatPlayerFragment;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;
import top.geek_studio.chenlongcould.musicplayer.util.ViewUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 * <p/>
 * Do not use {@link #setContentView(int)}. Instead wrap your layout with
 * {@link #wrapSlidingMusicPanel(int)} first and then return it in {@link #createContentView()}
 */
public abstract class AbsSlidingMusicPanelActivity extends AbsMusicServiceActivity
        implements SlidingUpPanelLayout.PanelSlideListener, CardPlayerFragment.Callbacks {

    /**
     * TAG
     */
    private static final String TAG = "AbsSlidingMusicPanel";

    @BindView(R.id.sliding_layout)
    SlidingUpPanelLayout slidingUpPanelLayout;

    private int navigationbarColor;

    /**
     * 卡片后台颜色
     */
    private int taskColor;

    /**
     * 是否亮色状态栏
     */
    private boolean lightStatusbar;

    /**
     * 存储正在播放的音乐的数据
     * */
    private NowPlayingScreen currentNowPlayingScreen;

    private AbsPlayerFragment playerFragment;

    private MiniPlayerFragment miniPlayerFragment;

    /**
     * 值动画
     * */
    private ValueAnimator navigationBarColorAnimator;

    /**
     * ARGB
     * */
    private ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    /**
     * 根 View
     */
    private View mRootView;

    /**
     * 覆盖模糊
     * */
    private RealtimeBlurView realtimeBlurView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRootView = createContentView();

        initBlur();

        setContentView(mRootView);
        ButterKnife.bind(this);

        // 获取上次播放得数据
        currentNowPlayingScreen = PreferenceUtil.getInstance(this).getNowPlayingScreen();

        Fragment fragment; // must implement AbsPlayerFragment

        // 上拉 Fragment 模式(样式)
        switch (currentNowPlayingScreen) {
            case FLAT:
                fragment = new FlatPlayerFragment();
                break;
            case CARD:
            default:
                fragment = new CardPlayerFragment();
                break;
        }

        // 上拉内容
        getSupportFragmentManager().beginTransaction().replace(R.id.player_fragment_container, fragment).commit();
        getSupportFragmentManager().executePendingTransactions();

        playerFragment = (AbsPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.player_fragment_container);
        miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.mini_player_fragment);

        // 设置点击展开 fragment
        if (miniPlayerFragment != null) {
            final View view = miniPlayerFragment.getView();
            if (view != null) view.setOnClickListener(v -> expandPanel());
        }

        // TODO: ？
        slidingUpPanelLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                slidingUpPanelLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                switch (getPanelState()) {
                    case EXPANDED:
                        onPanelSlide(slidingUpPanelLayout, 1);
                        onPanelExpanded(slidingUpPanelLayout);
                        break;
                    case COLLAPSED:
                        onPanelCollapsed(slidingUpPanelLayout);
                        break;
                    default:
                        playerFragment.onHide();
                        break;
                }
            }
        });

        // 添加监听
        slidingUpPanelLayout.addPanelSlideListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: ?
        if (currentNowPlayingScreen != PreferenceUtil.getInstance(this).getNowPlayingScreen()) {
            postRecreate();
        }
    }

    /**
     * 设置拖动视图
     * */
    public void setAntiDragView(View antiDragView) {
        slidingUpPanelLayout.setAntiDragView(antiDragView);
    }

    /**
     * 创建 view
     * */
    protected abstract View createContentView();

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

        if (!MusicPlayerRemote.getPlayingQueue().isEmpty()) {
            slidingUpPanelLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    slidingUpPanelLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    hideBottomBar(false);
                }
            });
        } // don't call hideBottomBar(true) here as it causes a bug with the SlidingUpPanelLayout
    }

    @Override
    public void onQueueChanged() {
        super.onQueueChanged();
        hideBottomBar(MusicPlayerRemote.getPlayingQueue().isEmpty());
    }

    /**
     * 当面板滑动时回调
     *
     * @param panel 面板
     * @param slideOffset 偏移值
     * */
    @Override
    public void onPanelSlide(View panel, @FloatRange(from = 0, to = 1) float slideOffset) {

        // 设置模糊
        setBlur(slideOffset, true);

        // 设置根 view 位置偏移
        translationRootView(slideOffset, 'y');

        // 设置 mini 播放器不透明度
        setMiniPlayerAlphaProgress(slideOffset);

        // 取消动画，重新执行新的颜色过渡
        if (navigationBarColorAnimator != null) navigationBarColorAnimator.cancel();
        super.setNavigationbarColor((int) argbEvaluator.evaluate(slideOffset, navigationbarColor, playerFragment.getPaletteColor()));
    }

    /**
     * 初始化 BlurView
     */
    private void initBlur() {
        realtimeBlurView = mRootView.findViewById(R.id.real_blur);
        realtimeBlurView.setVisibility(View.GONE);

        if (PreferenceUtil.getInstance(getApplicationContext()).isUseBlur()) {
            // set realtimeBlur
            realtimeBlurView.setBlurRadius(150);
            realtimeBlurView.setDownsampleFactor(0.1f);
        }
    }

    /**
     * 设置覆盖模糊
     *
     * @param slideOffset 滑动偏移值
     * @param transition  是否将 Activity ROOT view 位移
     */
    protected void setBlur(float slideOffset, boolean transition) {
        if (!PreferenceUtil.getInstance(getApplicationContext()).isUseBlur()) return;

        if (slideOffset > 0) realtimeBlurView.setVisibility(View.VISIBLE);
        if (slideOffset == 0) realtimeBlurView.setVisibility(View.GONE);

        if (transition) realtimeBlurView.setTranslationY(0 - slideOffset * 120);
        realtimeBlurView.setAlpha((float) (slideOffset * 1.5));
//        realtimeBlurView.setBlurRadius(slideOffset * 50);
    }

    /**
     * 移动 ROOT view
     *
     * @param slideOffset 偏移值
     * @param target      目标 (xyz)
     */
    protected void translationRootView(float slideOffset, char target) {
        if (target == 'y' || target == 'Y')
            mRootView.findViewById(R.id.content_container).setTranslationY(0 - slideOffset * 120);
        if (target == 'x' || target == 'X')
            mRootView.findViewById(R.id.content_container).setTranslationX(0 + slideOffset * 120);
    }

    /**
     * 滑动面板状态改变
     *
     * @param panel 面板
     * @param previousState 之前状态
     * @param newState 现在状态
     * */
    @Override
    public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
        switch (newState) {
            case COLLAPSED:
//                Log.d(TAG, "onPanelStateChanged: COLLAPSED");
                onPanelCollapsed(panel);

                // 检测模糊
                if (PreferenceUtil.getInstance(getApplicationContext()).isUseBlur())
                    realtimeBlurView.setVisibility(View.GONE);
                break;
            case EXPANDED:
//                Log.d(TAG, "onPanelStateChanged: EXPANDED");
                onPanelExpanded(panel);

                // 检测模糊
                if (PreferenceUtil.getInstance(getApplicationContext()).isUseBlur())
                    realtimeBlurView.setVisibility(View.VISIBLE);
                break;
            case ANCHORED:
                collapsePanel(); // this fixes a bug where the panel would get stuck for some reason
                break;
        }
    }

    /**
     * 面板被折叠
     *
     * @param panel 面板
     * */
    public void onPanelCollapsed(View panel) {
        // restore values
        super.setLightStatusbar(lightStatusbar);
        super.setTaskDescriptionColor(taskColor);
        super.setNavigationbarColor(navigationbarColor);

        playerFragment.setMenuVisibility(false);
        playerFragment.setUserVisibleHint(false);
        playerFragment.onHide();
    }

    /**
     * 面板展开
     *
     * @param panel 面板
     * */
    public void onPanelExpanded(View panel) {
        // setting fragments values
        int playerFragmentColor = playerFragment.getPaletteColor();
        super.setLightStatusbar(false);
        super.setTaskDescriptionColor(playerFragmentColor);
        super.setNavigationbarColor(playerFragmentColor);

        playerFragment.setMenuVisibility(true);
        playerFragment.setUserVisibleHint(true);
        playerFragment.onShow();
    }

    /**
     * 设置 mini player alpha 值, 并进行判断是否需要隐藏
     *
     * @param progress 值
     * */
    private void setMiniPlayerAlphaProgress(@FloatRange(from = 0, to = 1) float progress) {
        if (miniPlayerFragment.getView() == null) return;
        float alpha = 1 - progress;
        miniPlayerFragment.getView().setAlpha(alpha);
        // necessary to make the views below clickable
        miniPlayerFragment.getView().setVisibility(alpha == 0 ? View.GONE : View.VISIBLE);
    }

    /**
     * 获取面板状态
     *
     * @return 面板状态
     * */
    public SlidingUpPanelLayout.PanelState getPanelState() {
        return slidingUpPanelLayout == null ? null : slidingUpPanelLayout.getPanelState();
    }

    /**
     * 折叠面板
     * */
    public void collapsePanel() {
        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    /**
     * 展开面板
     * */
    public void expandPanel() {
        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
    }

    /**
     * 隐藏上拉
     *
     * @param hide 隐藏上拉
     * */
    public void hideBottomBar(final boolean hide) {
        if (hide) {
            slidingUpPanelLayout.setPanelHeight(0);
            collapsePanel();
        } else {
            slidingUpPanelLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.mini_player_height));
        }
    }

    protected View wrapSlidingMusicPanel(@LayoutRes int resId) {
        @SuppressLint("InflateParams")
        View slidingMusicPanelLayout = getLayoutInflater().inflate(R.layout.sliding_music_panel_layout, null);
        ViewGroup contentContainer = slidingMusicPanelLayout.findViewById(R.id.content_container);
        getLayoutInflater().inflate(resId, contentContainer);
        return slidingMusicPanelLayout;
    }

    /**
     * 返回键
     * */
    @Override
    public void onBackPressed() {
        if (!handleBackPress()) super.onBackPressed();
    }

    /**
     * 处理返回
     */
    public boolean handleBackPress() {
        if (slidingUpPanelLayout.getPanelHeight() != 0 && playerFragment.onBackPressed())
            return true;
        if (getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            collapsePanel();
            return true;
        }

        return false;
    }

    /**
     * 调色盘颜色变更回调
     * */
    @Override
    public void onPaletteColorChanged() {
        if (getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            int playerFragmentColor = playerFragment.getPaletteColor();
            super.setTaskDescriptionColor(playerFragmentColor);
            animateNavigationBarColor(playerFragmentColor);
        }
    }

    /**
     * 设置状态颜色模式
     *
     * @param enabled isLight
     * */
    @Override
    public void setLightStatusbar(boolean enabled) {
        lightStatusbar = enabled;
        if (getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            super.setLightStatusbar(enabled);
        }
    }

    /**
     * 设置导航栏颜色
     *
     * @param color 颜色
     * */
    @Override
    public void setNavigationbarColor(int color) {
        this.navigationbarColor = color;
        if (getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            if (navigationBarColorAnimator != null) navigationBarColorAnimator.cancel();
            super.setNavigationbarColor(color);
        }
    }

    /**
     * 导航栏颜色动画
     *
     * @param color 目标色
     * */
    private void animateNavigationBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (navigationBarColorAnimator != null) navigationBarColorAnimator.cancel();
            navigationBarColorAnimator = ValueAnimator
                    .ofArgb(getWindow().getNavigationBarColor(), color)
                    .setDuration(ViewUtil.PHONOGRAPH_ANIM_TIME);
            navigationBarColorAnimator.setInterpolator(new PathInterpolator(0.4f, 0f, 1f, 1f));
            navigationBarColorAnimator.addUpdateListener(animation -> AbsSlidingMusicPanelActivity.super.setNavigationbarColor((Integer) animation.getAnimatedValue()));
            navigationBarColorAnimator.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (navigationBarColorAnimator != null) navigationBarColorAnimator.cancel(); // just in case
    }

    @Override
    public void setTaskDescriptionColor(@ColorInt int color) {
        this.taskColor = color;
        if (getPanelState() == null || getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            super.setTaskDescriptionColor(color);
        }
    }

    @Override
    protected View getSnackBarContainer() {
        return findViewById(R.id.content_container);
    }

    public SlidingUpPanelLayout getSlidingUpPanelLayout() {
        return slidingUpPanelLayout;
    }

    public MiniPlayerFragment getMiniPlayerFragment() {
        return miniPlayerFragment;
    }

    public AbsPlayerFragment getPlayerFragment() {
        return playerFragment;
    }
}
