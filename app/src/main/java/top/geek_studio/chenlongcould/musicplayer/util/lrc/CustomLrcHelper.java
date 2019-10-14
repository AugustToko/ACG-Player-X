package top.geek_studio.chenlongcould.musicplayer.util.lrc;

import androidx.annotation.NonNull;

import com.lauzy.freedom.library.Lrc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
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
    //[03:56.00][03:18.00][02:06.00][01:07.00]原谅我这一生不羁放纵爱自由
    private static final String LINE_REGEX = "((\\[\\d{2}:\\d{2}\\.\\d{2}])+)(.*)";
    private static final String TIME_REGEX = "\\[(\\d{2}):(\\d{2})\\.(\\d{2})]";

    public static List<Lrc> getLrc(@NonNull String lrcString) throws IOException {
        List<Lrc> lrcs = new ArrayList<>();

        ByteArrayInputStream is=new ByteArrayInputStream(lrcString.getBytes());
        BufferedReader br=new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = br.readLine()) != null) {
            List<Lrc> lrcList = parseLrc(line);
            if (lrcList != null && lrcList.size() != 0) {
                lrcs.addAll(lrcList);
            }
        }
        sortLrcs(lrcs);
        return lrcs;
    }

    private static void sortLrcs(List<Lrc> lrcs) {
        Collections.sort(lrcs, new Comparator<Lrc>() {
            @Override
            public int compare(Lrc o1, Lrc o2) {
                return (int) (o1.getTime() - o2.getTime());
            }
        });
    }

    private static List<Lrc> parseLrc(String lrcLine) {
        if (lrcLine.trim().isEmpty()) {
            return null;
        }
        List<Lrc> lrcs = new ArrayList<>();
        Matcher matcher = Pattern.compile(LINE_REGEX).matcher(lrcLine);
        if (!matcher.matches()) {
            return null;
        }

        String time = matcher.group(1);
        String content = matcher.group(3);
        Matcher timeMatcher = Pattern.compile(TIME_REGEX).matcher(time);

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

    public static String formatTime(long time) {
        int min = (int) (time / 60000);
        int sec = (int) (time / 1000 % 60);
        return adjustFormat(min) + ":" + adjustFormat(sec);
    }

    private static String adjustFormat(int time) {
        if (time < 10) {
            return "0" + time;
        }
        return time + "";
    }
}
