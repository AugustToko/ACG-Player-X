package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity;

import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import top.geek_studio.chenlongcould.musicplayer.ui.activities.MainActivity;

/**
 * BaseFragment
 *
 * @author Karim Abou Zeid (kabouzeid)
 * @see top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.LibraryFragment
 * @see top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.folders.FoldersFragment
 */
public abstract class AbsMainActivityFragment extends Fragment implements MainActivity.MainActivityFragmentCallbacks {

    protected ViewGroup root;

    public MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = createRootView(inflater, container, savedInstanceState);
        return root;
    }

    protected abstract ViewGroup createRootView(@NotNull @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    @Override
    public void hide(@Nullable AnimatorListenerAdapter adapter) {
        if (root != null) {
            root.animate().alphaBy(100).alpha(0).setListener(adapter).start();
        }
    }

    @Override
    public void show(@Nullable AnimatorListenerAdapter adapter) {
        if (root != null) {
            root.animate().alphaBy(0).alpha(100).setListener(adapter).start();
        }
    }
}
