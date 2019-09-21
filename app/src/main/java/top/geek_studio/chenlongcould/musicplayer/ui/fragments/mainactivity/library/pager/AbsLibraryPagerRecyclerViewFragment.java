package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import top.geek_studio.chenlongcould.musicplayer.util.ViewUtil;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.chenlongcould.musicplayer.R;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * @param <A> 适配器类型
 * @param <LM> {@link androidx.recyclerview.widget.RecyclerView.LayoutManager} 类型
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public abstract class AbsLibraryPagerRecyclerViewFragment<A extends RecyclerView.Adapter, LM extends RecyclerView.LayoutManager> extends AbsLibraryPagerFragment implements OnOffsetChangedListener {

    private Unbinder unbinder;

    @BindView(R.id.container)
    View container;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @Nullable
    @BindView(android.R.id.empty)
    TextView empty;

    private A adapter;
    private LM layoutManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(getLayoutRes(), container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 监听
        getLibraryFragment().addOnAppBarOffsetChangedListener(this);

        initLayoutManager();
        initAdapter();
        setUpRecyclerView();
    }

    /**
     * 设置 {@link RecyclerView}
     */
    private void setUpRecyclerView() {
        Activity activity = getActivity();
        if (recyclerView instanceof FastScrollRecyclerView && activity != null) {
            // 更新颜色
            ViewUtil.setUpFastScrollRecyclerViewColor(getActivity(), ((FastScrollRecyclerView) recyclerView), ThemeStore.accentColor(activity));
        }
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    /**
     * 刷新 {@link androidx.recyclerview.widget.RecyclerView.LayoutManager}
     */
    protected void invalidateLayoutManager() {
        initLayoutManager();
        recyclerView.setLayoutManager(layoutManager);
    }

    /**
     * 刷新 {@link FastScrollRecyclerView.Adapter}
     */
    protected void invalidateAdapter() {
        initAdapter();
        checkIsEmpty();
        recyclerView.setAdapter(adapter);
    }

    /**
     * 初始化适配器
     * */
    private void initAdapter() {
        adapter = createAdapter();
        // 数据监听
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkIsEmpty();
            }
        });
    }

    private void initLayoutManager() {
        layoutManager = createLayoutManager();
    }

    protected A getAdapter() {
        return adapter;
    }

    protected LM getLayoutManager() {
        return layoutManager;
    }

    protected RecyclerView getRecyclerView() {
        return recyclerView;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        container.setPadding(container.getPaddingLeft(), container.getPaddingTop(), container.getPaddingRight(), getLibraryFragment().getTotalAppBarScrollingRange() + i);
    }

    /**
     * 如果歌曲为空, 设置提示文字
     * */
    private void checkIsEmpty() {
        if (empty != null) {
            empty.setText(getEmptyMessage());
            empty.setVisibility(adapter == null || adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    @StringRes
    protected int getEmptyMessage() {
        return R.string.empty;
    }

    @LayoutRes
    protected int getLayoutRes() {
        return R.layout.fragment_main_activity_recycler_view;
    }

    /**
     * 创建布局管理
     * */
    protected abstract LM createLayoutManager();

    /**
     * 创建适配器,
     * (子类实现)
     * */
    @NonNull
    protected abstract A createAdapter();

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 移除监听
        getLibraryFragment().removeOnAppBarOffsetChangedListener(this);
        unbinder.unbind();
    }
}
