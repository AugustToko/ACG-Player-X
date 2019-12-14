package top.geek_studio.chenlongcould.musicplayer.util.lrc;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.lauzy.freedom.library.Lrc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : chenlongcould
 * @date : 2019/10/14/10
 */
public class CustomLrcHelper {
    private static final String CHARSET = "utf-8";
    //[03:56.00]hhhhh
    private static final String LINE_REGEX = "((\\[\\d{2}:\\d{2}\\.\\d{2}])+)(.*)";
    private static final String TIME_REGEX = "\\[(\\d{2}):(\\d{2})\\.(\\d{2})]";

    private static final String TAG = CustomLrcHelper.class.getSimpleName();

    public static List<Lrc> getLrc(@Nullable String lrcString) throws IOException {
        if (TextUtils.isEmpty(lrcString)) return new ArrayList<>();

        final List<Lrc> lrcs = new ArrayList<>();

        final ByteArrayInputStream is = new ByteArrayInputStream(lrcString.getBytes());
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = br.readLine()) != null) {
            // 检测第十个字符是否为 ]
            // 应对 [03:56.000]hhhhh 这种类型的时间

            if (line.length() < 9) break;

            if (line.charAt(9) != ']') {
                final StringBuilder builder = new StringBuilder(line);
                builder.deleteCharAt(9);
                line = builder.toString();
            }

            final List<Lrc> lrcList = parseLrc(line);
            if (lrcList != null && lrcList.size() != 0) {
                lrcs.addAll(lrcList);
            }
        }

        sortLrcs(lrcs);
        return lrcs;
    }

    private static void sortLrcs(List<Lrc> lrcs) {
        Collections.sort(lrcs, (o1, o2) -> (int) (o1.getTime() - o2.getTime()));
    }

    private static List<Lrc> parseLrc(String lrcLine) {
        if (lrcLine.trim().isEmpty()) {
            return null;
        }

        final List<Lrc> lrcs = new ArrayList<>();
        final Matcher matcher = Pattern.compile(LINE_REGEX).matcher(lrcLine);
        if (!matcher.matches()) {
            return null;
        }

        final String time = matcher.group(1);
        final String content = matcher.group(3);

        final Matcher timeMatcher = Pattern.compile(TIME_REGEX).matcher(time);

        while (timeMatcher.find()) {
            String min = timeMatcher.group(1);
            String sec = timeMatcher.group(2);
            String mil = timeMatcher.group(3);
            Lrc lrc = new Lrc();
            if (content != null && content.length() != 0) {
                lrc.setTime(Long.parseLong(min) * 60 * 1000 + Long.parseLong(sec) * 1000
                        + Long.parseLong(mil) * 10);
                lrc.setText(content);
                lrcs.add(lrc);
            }
        }
        return lrcs;
    }

//    public static String formatTime(long time) {
//        int min = (int) (time / 60000);
//        int sec = (int) (time / 1000 % 60);
//        return adjustFormat(min) + ":" + adjustFormat(sec);
//    }
//
//    private static String adjustFormat(int time) {
//        if (time < 10) {
//            return "0" + time;
//        }
//        return time + "";
//    }
}
