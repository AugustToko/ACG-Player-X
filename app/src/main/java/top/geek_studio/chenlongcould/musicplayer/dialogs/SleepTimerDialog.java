package top.geek_studio.chenlongcould.musicplayer.dialogs;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.ThemeSingleton;
import com.kabouzeid.chenlongcould.musicplayer.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import top.geek_studio.chenlongcould.musicplayer.App;
import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.service.MusicService;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.PurchaseActivity;
import top.geek_studio.chenlongcould.musicplayer.util.MusicUtil;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
//TODO: 修改
public class SleepTimerDialog extends DialogFragment {
//    @BindView(R.id.seek_arc)
//    SeekArc seekArc;
//    @BindView(R.id.timer_display)
//    TextView timerDisplay;
//    @BindView(R.id.should_finish_last_song)
//    CheckBox shouldFinishLastSong;
//
//    private int seekArcProgress;
//    private MaterialDialog materialDialog;
//    private TimerUpdater timerUpdater;
//
//    @Override
//    public void onDismiss(DialogInterface dialog) {
//        super.onDismiss(dialog);
//        timerUpdater.cancel();
//    }
//
//    @NonNull
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        timerUpdater = new TimerUpdater();
//        materialDialog = new MaterialDialog.Builder(getActivity())
//                .title(getActivity().getResources().getString(R.string.action_sleep_timer))
//                .positiveText(R.string.action_set)
//                .onPositive((dialog, which) -> {
//                    if (getActivity() == null) {
//                        return;
//                    }
//                    if (!App.isProVersion()) {
//                        Toast.makeText(getActivity(), getString(R.string.sleep_timer_is_a_pro_feature), Toast.LENGTH_LONG).show();
//                        startActivity(new Intent(getContext(), PurchaseActivity.class));
//                        return;
//                    }
//
//                    PreferenceUtil.getInstance(getActivity()).setSleepTimerFinishMusic(shouldFinishLastSong.isChecked());
//
//                    final int minutes = seekArcProgress;
//
//                    PendingIntent pi = makeTimerPendingIntent(PendingIntent.FLAG_CANCEL_CURRENT);
//
//                    final long nextSleepTimerElapsedTime = SystemClock.elapsedRealtime() + minutes * 60 * 1000;
//                    PreferenceUtil.getInstance(getActivity()).setNextSleepTimerElapsedRealtime(nextSleepTimerElapsedTime);
//                    AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
//                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextSleepTimerElapsedTime, pi);
//
//                    Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.sleep_timer_set, minutes), Toast.LENGTH_SHORT).show();
//                })
//                .onNeutral((dialog, which) -> {
//                    if (getActivity() == null) {
//                        return;
//                    }
//                    final PendingIntent previous = makeTimerPendingIntent(PendingIntent.FLAG_NO_CREATE);
//                    if (previous != null) {
//                        AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
//                        am.cancel(previous);
//                        previous.cancel();
//                        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.sleep_timer_canceled), Toast.LENGTH_SHORT).show();
//                    }
//
//                    MusicService musicService = MusicPlayerRemote.musicService;
//                    if (musicService != null && musicService.pendingQuit) {
//                        musicService.pendingQuit = false;
//                        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.sleep_timer_canceled), Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .showListener(dialog -> {
//                    if (makeTimerPendingIntent(PendingIntent.FLAG_NO_CREATE) != null) {
//                        timerUpdater.start();
//                    }
//                })
//                .customView(R.layout.dialog_sleep_timer, false)
//                .build();
//
//        if (getActivity() == null || materialDialog.getCustomView() == null) {
//            return materialDialog;
//        }
//
//        ButterKnife.bind(this, materialDialog.getCustomView());
//
//        boolean finishMusic = PreferenceUtil.getInstance(getActivity()).getSleepTimerFinishMusic();
//        shouldFinishLastSong.setChecked(finishMusic);
//
//        seekArc.setProgressColor(ThemeSingleton.get().positiveColor.getDefaultColor());
//        seekArc.setThumbColor(ThemeSingleton.get().positiveColor.getDefaultColor());
//
//        seekArc.post(() -> {
//            int width = seekArc.getWidth();
//            int height = seekArc.getHeight();
//            int small = Math.min(width, height);
//
//            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(seekArc.getLayoutParams());
//            layoutParams.height = small;
//            seekArc.setLayoutParams(layoutParams);
//        });
//
//        seekArcProgress = PreferenceUtil.getInstance(getActivity()).getLastSleepTimerValue();
//        updateTimeDisplayTime();
//        seekArc.setProgress(seekArcProgress);
//
//        seekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
//            @Override
//            public void onProgressChanged(@NonNull SeekArc seekArc, int i, boolean b) {
//                if (i < 1) {
//                    seekArc.setProgress(1);
//                    return;
//                }
//                seekArcProgress = i;
//                updateTimeDisplayTime();
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekArc seekArc) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekArc seekArc) {
//                PreferenceUtil.getInstance(getActivity()).setLastSleepTimerValue(seekArcProgress);
//            }
//        });
//
//        return materialDialog;
//    }
//
//    private void updateTimeDisplayTime() {
//        timerDisplay.setText(seekArcProgress + " min");
//    }
//
//    private PendingIntent makeTimerPendingIntent(int flag) {
//        return PendingIntent.getService(getActivity(), 0, makeTimerIntent(), flag);
//    }
//
//    private Intent makeTimerIntent() {
//        Intent intent = new Intent(getActivity(), MusicService.class);
//        if (shouldFinishLastSong.isChecked()) {
//            return intent.setAction(MusicService.ACTION_PENDING_QUIT);
//        }
//        return intent.setAction(MusicService.ACTION_QUIT);
//    }
//
//    private void updateCancelButton() {
//        MusicService musicService = MusicPlayerRemote.musicService;
//        if (musicService != null && musicService.pendingQuit) {
//            materialDialog.setActionButton(DialogAction.NEUTRAL, materialDialog.getContext().getString(R.string.cancel_current_timer));
//        } else {
//            materialDialog.setActionButton(DialogAction.NEUTRAL, null);
//        }
//    }
//
//    private class TimerUpdater extends CountDownTimer {
//        public TimerUpdater() {
//            super(PreferenceUtil.getInstance(getActivity()).getNextSleepTimerElapsedRealTime() - SystemClock.elapsedRealtime(), 1000);
//        }
//
//        @Override
//        public void onTick(long millisUntilFinished) {
//            materialDialog.setActionButton(DialogAction.NEUTRAL, materialDialog.getContext().getString(R.string.cancel_current_timer) + " (" + MusicUtil.getReadableDurationString(millisUntilFinished) + ")");
//        }
//
//        @Override
//        public void onFinish() {
//            updateCancelButton();
//        }
//    }
}
