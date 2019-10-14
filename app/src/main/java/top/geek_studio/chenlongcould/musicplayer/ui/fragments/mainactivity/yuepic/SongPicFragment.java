package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.yuepic;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kabouzeid.appthemehelper.common.views.ATEPrimaryTextView;
import com.kabouzeid.appthemehelper.common.views.ATESecondaryTextView;
import com.kabouzeid.chenlongcould.musicplayer.R;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import top.geek_studio.chenlongcould.musicplayer.interfaces.TransDataCallback;
import top.geek_studio.chenlongcould.musicplayer.model.DataViewModel;
import top.geek_studio.chenlongcould.musicplayer.model.yuepic.YuePic;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.AbsMainActivityFragment;
import top.geek_studio.chenlongcould.musicplayer.util.openimage.YuePicUtil;

/**
 * HomePage
 *
 * @author : chenlongcould
 * @date : 2019/10/03/10
 */
public class SongPicFragment extends AbsMainActivityFragment {

    private static final String TAG = SongPicFragment.class.getSimpleName();

    private Unbinder unbinder;

    @BindView(R.id.pic)
    AppCompatImageView imageView;

    @BindView(R.id.yuePicAuthorName)
    ATEPrimaryTextView yuePicAuthorName;

    @BindView(R.id.yuePicUrl)
    ATESecondaryTextView yuePicUrl;

    @BindView(R.id.changeImage)
    FloatingActionButton changeImageButton;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private DataViewModel dataViewModel;

    public static SongPicFragment newInstance() {
        return new SongPicFragment();
    }

    @Override
    protected ViewGroup createRootView(@NotNull @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_yuepic, container, false);
        unbinder = ButterKnife.bind(this, view);
        getMainActivity().setSupportActionBar(toolbar);
        getMainActivity().getSupportActionBar().setElevation(0f);
        final View statusBar = getMainActivity().getWindow().getDecorView().getRootView().findViewById(R.id.status_bar);
        statusBar.setVisibility(View.GONE);
        return (ViewGroup) view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        dataViewModel = ViewModelProviders.of(getMainActivity()).get(DataViewModel.class);

        changeImageButton.setOnClickListener(v -> {
            final Boolean allow = dataViewModel.allowGetYuePic.getValue();

            if (allow == null || allow) {
                loadYuePicFromNet();
            } else {
                Toast.makeText(getMainActivity(), "Wait...", Toast.LENGTH_SHORT).show();
            }

        });

        setUpYuePic();
    }

    private void setUpYuePic() {
        final YuePic y = dataViewModel.yuePicData.getValue();
        if (y != null) {
            putIntoView(y);
            return;
        }

        CustomThreadPool.post(() -> {
            final YuePic yuePic = YuePicUtil.readYuePicFile(getMainActivity());
            if (yuePic != null) {
                Log.d(TAG, "setUpYuePic: load from file");
                getMainActivity().runOnUiThread(() -> {
                    putIntoView(yuePic);
                    dataViewModel.yuePicData.postValue(yuePic);
                });
            } else {
                Log.d(TAG, "setUpYuePic: load from net");
                loadYuePicFromNet();
            }
        });
    }

    private void loadYuePicFromNet() {
        YuePicUtil.getRandomYuePic(getMainActivity(), new TransDataCallback<YuePic>() {
            @Override
            public void onTrans(@NonNull YuePic data) {
                getMainActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        putIntoView(data);

                        dataViewModel.yuePicData.setValue(data);
                        // 每隔 3s 允许获取一次
                        dataViewModel.allowGetYuePic.setValue(false);
                    }
                });

                imageView.postDelayed(() -> dataViewModel.allowGetYuePic.setValue(true), 3000);
                // save file
                CustomThreadPool.post(() -> YuePicUtil.saveYuePicFile(getMainActivity(), data));
            }

            @Override
            public void onError() {
                // ...
                Toast.makeText(getMainActivity(), "Get YuePic -> Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @UiThread
    private void putIntoView(@NonNull YuePic data) {
        if (!isAdded()) return;
        Glide.with(getMainActivity()).load(data.getUrls().getRegular()).into(imageView);
        yuePicAuthorName.setText(data.getUser().getName());
        yuePicUrl.setText(data.getLinks().getHtml());
    }

    //    @Nullable
//    public static String stringMD5(String input) {
//
//        try {
//
//            // 拿到一个MD5转换器（如果想要SHA1参数换成”SHA1”）
//
//            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
//
//
//            // 输入的字符串转换成字节数组
//
//            byte[] inputByteArray = input.getBytes();
//
//
//            // inputByteArray是输入字符串转换得到的字节数组
//
//            messageDigest.update(inputByteArray);
//
//
//            // 转换并返回结果，也是字节数组，包含16个元素
//
//            byte[] resultByteArray = messageDigest.digest();
//
//
//            // 字符数组转换成字符串返回
//
//            return byteArrayToHex(resultByteArray);
//
//
//        } catch (NoSuchAlgorithmException e) {
//
//            return null;
//        }
//
//    }
//
//    public static String byteArrayToHex(byte[] byteArray) {
//
//        // 首先初始化一个字符数组，用来存放每个16进制字符
//
//        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
//
//
//        // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
//
//        char[] resultCharArray = new char[byteArray.length * 2];
//
//
//        // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
//
//        int index = 0;
//
//        for (byte b : byteArray) {
//
//            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
//
//            resultCharArray[index++] = hexDigits[b & 0xf];
//
//        }
//
//        // 字符数组组合成字符串返回
//
//        return new String(resultCharArray);
//
//    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        final View statusBar = getMainActivity().getWindow().getDecorView().getRootView().findViewById(R.id.status_bar);
        statusBar.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean handleBackPress() {
        return false;
    }

}
