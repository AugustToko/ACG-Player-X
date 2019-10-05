package top.geek_studio.chenlongcould.musicplayer.adapter;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kabouzeid.appthemehelper.common.views.ATEPrimaryTextView;
import com.kabouzeid.appthemehelper.common.views.ATESecondaryTextView;
import com.kabouzeid.chenlongcould.musicplayer.R;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import top.geek_studio.chenlongcould.musicplayer.glide.UrlGlideRequest;
import top.geek_studio.chenlongcould.musicplayer.model.NetMusicComment;
import top.geek_studio.chenlongcould.musicplayer.util.OkHttpUtils;
import top.geek_studio.chenlongcould.musicplayer.views.CircularImageView;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class MusicCommentAdapter extends RecyclerView.Adapter<MusicCommentAdapter.ViewHolder> {

    private static final String TAG = MusicCommentAdapter.class.getSimpleName();

    protected final AppCompatActivity activity;

    protected List<NetMusicComment.HotCommentsBean> dataSet;

    protected int itemLayoutRes;

    public MusicCommentAdapter(AppCompatActivity activity, List<NetMusicComment.HotCommentsBean> dataSet, @LayoutRes int itemLayoutRes) {
        this.activity = activity;
        this.dataSet = dataSet;
        this.itemLayoutRes = itemLayoutRes;
        setHasStableIds(true);
    }

    public void swapDataSet(List<NetMusicComment.HotCommentsBean> dataSet) {
        this.dataSet = dataSet;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return dataSet.get(position).getCommentId();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(dataSet.get(position).getContent());
        holder.userName.setText(dataSet.get(position).getUser().getNickname());

        final NetMusicComment.HotCommentsBean bean = dataSet.get(position);

        UrlGlideRequest.Builder.from(Glide.with(activity), new UrlGlideRequest.UrlAndId(bean.getCommentId(), bean.getUser().getAvatarUrl()))
                .setDefaultImage(R.drawable.ic_person_flat)
                .asBitmap()
                .build()
                .into(holder.imageView);

    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        CircularImageView imageView;

        ATESecondaryTextView textView;

        ATEPrimaryTextView userName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.userImage);
            textView = itemView.findViewById(R.id.commentText);
            userName = itemView.findViewById(R.id.userName);
        }
    }
}
