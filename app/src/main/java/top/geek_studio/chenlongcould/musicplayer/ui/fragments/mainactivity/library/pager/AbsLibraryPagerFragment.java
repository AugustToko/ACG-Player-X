package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.ui.fragments.AbsMusicServiceFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.LibraryFragment;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public abstract class AbsLibraryPagerFragment extends AbsMusicServiceFragment {

    private static final String TAG = AbsLibraryPagerFragment.class.getSimpleName();


    public LibraryFragment getLibraryFragment() {
        return (LibraryFragment) getParentFragment();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // update menu
        setHasOptionsMenu(true);
    }

    /**
     * 获取摘要信息，用于填充 {@link android.widget.Toolbar#setSubtitle(CharSequence)}
     *
     * @return data
     */
    public abstract String getSubTitle();

    public class CallbackInfo<T> {
        private List<T> callbacks = new ArrayList<>();

        public List<T> getCallbacks() {
            return callbacks;
        }

        public List<T> addCallbacks(T callback) {
            callbacks.add(callback);
            return callbacks;
        }
    }
}
