package top.geek_studio.chenlongcould.musicplayer.model.nmlrc;

/**
 * 网易云音乐 LRC
 *
 * for <a href="https://i.a632079.me"></a>
 *
 * @author : chenlongcould
 * @date : 2019/10/13/20
 */
public class NmLrc {

    /**
     * sgc : false
     * sfy : false
     * qfy : false
     * transUser : {"id":28391863,"status":99,"demand":1,"userid":35941609,"nickname":"希Home-0869","uptime":1459221362887}
     * lyricUser : {"id":28391863,"status":99,"demand":0,"userid":34214008,"nickname":"不林塔","uptime":1445787345526}
     * lrc : {"version":5,"lyric":"[00:00.000] 作曲 : 赤髪\n[00:01.000] 作词 : 赤髪\n[00:23.34]遠く 遠く 記憶の奥に沈めた\n[00:34.04]思い出を 掬い（すくい）上げる\n[00:44.88]拙い（つたない） 会話 慣れない姿に頬を染め\n[00:55.74]賑わう（にぎわう）方へ ゆっくりと歩くんだ\n[01:06.82]終わる夕暮れ 空を見上げ 近づく影\n[01:17.71]ほら ほら 手が触れ合って 気づけば\n[01:26.98]不器用に握った\n[01:33.71]高く空に 打ち上がり 咲いた\n[01:43.51]一瞬だけ キミを照らして 消えるんだ\n[01:56.33]嬉しそうにはしゃぐ横顔に\n[02:06.33]見惚れ焼きつく 記憶をぎゅっと\n[02:11.92]いつまでも 強く 強く この手握っていて\n[02:29.87]ひとつ ひとつ 描かれた歴史の欠片を\n[02:40.48]記憶へ沈め 抱きしめて 眠った\n[02:51.04]通り雨を 追いかけるように キミが現れ（あらわれ）\n[03:02.10]傘に入れてあげるよ 微笑み 頬を伝う雨\n[03:18.58]すべて世界がスローに写る（うつる）程\n[03:28.16]キミの言葉は 私への愛で満ちていて\n[03:39.95]抗う隙さえ与える間もなく\n[03:49.95]触れる息は 心もとなく 弱って\n[03:57.09]薄く 薄く キミが消えていく\n[04:06.74]キミが消えていく\n[04:12.59]流れる雨\n[04:22.74]止めどなく溢れ\n[04:41.55]土砂降りの雨はキミを通りぬけ\n[04:51.35]遠くの空は 嘘みたいに晴れていく\n[05:02.95]この世界がすべて書き換わり\n[05:13.10]キミの存在 記憶 すべて 消し去っても\n[05:20.70]消せない 消せない はしゃぐ笑顔\n[05:25.46]消せはしない キミを想っている\n[05:31.90]\n"}
     * klyric : {"version":0,"lyric":null}
     * tlyric : {"version":2,"lyric":"[by:希Home-0869]\n[00:23.34]沉浸在遥远的记忆深处\n[00:34.04]从其中掬出一捧回忆\n[00:44.88]笨拙的交谈  因还未能习惯的姿态  双颊染上绯红\n[00:55.74]慢步走向那热闹的地方\n[01:06.82]抬头仰望夕阳   渐渐靠近的身影\n[01:17.71]你看  发现触碰到一起的双手\n[01:26.98]没志气的握紧\n[01:33.71]高空中升起盛开的烟火\n[01:43.51]瞬间照亮你的脸庞又转瞬消失不见\n[01:56.33]看上去很开心欢喜的你的侧脸\n[02:06.33]恍惚的注视着你  将这记忆铭记于心\n[02:11.92]无论何时都会紧紧握住这双手\n[02:29.87]一片一片     将描绘往昔的断片\n[02:40.48]沉入记忆  拥紧而眠\n[02:51.04]想要追赶阵雨时  你悄然出现\n[03:02.10]我来给你撑伞吧   雨水沿着笑脸低落\n[03:18.58]整个世界仿佛都缓慢下来\n[03:28.16]你的话语中充满了对我的爱\n[03:39.95]连反抗的时间都不曾给过我\n[03:49.95]相触的气息  心跳微弱\n[03:57.09]逐渐稀薄   你就这样消失\n[04:06.74]你就这样消失\n[04:12.59]流淌的雨水\n[04:22.74]不停的流下\n[04:41.55]倾盆大雨淋透你的身体\n[04:51.35]遥远的天空  不真实的开始放晴\n[05:02.95]这个世界已经重新书写\n[05:13.10]即使你的存在 你的记忆 全部消逝\n[05:20.70]欢笑的笑颜  永远不会消失\n[05:25.46]对你的想念  永远不会消失\n"}
     * code : 200
     */

    private boolean sgc;
    private boolean sfy;
    private boolean qfy;
    private TransUserBean transUser;
    private LyricUserBean lyricUser;
    private LrcBean lrc;
    private KlyricBean klyric;
    private TlyricBean tlyric;
    private int code;

    public boolean isSgc() {
        return sgc;
    }

    public void setSgc(boolean sgc) {
        this.sgc = sgc;
    }

    public boolean isSfy() {
        return sfy;
    }

    public void setSfy(boolean sfy) {
        this.sfy = sfy;
    }

    public boolean isQfy() {
        return qfy;
    }

    public void setQfy(boolean qfy) {
        this.qfy = qfy;
    }

    public TransUserBean getTransUser() {
        return transUser;
    }

    public void setTransUser(TransUserBean transUser) {
        this.transUser = transUser;
    }

    public LyricUserBean getLyricUser() {
        return lyricUser;
    }

    public void setLyricUser(LyricUserBean lyricUser) {
        this.lyricUser = lyricUser;
    }

    public LrcBean getLrc() {
        return lrc;
    }

    public void setLrc(LrcBean lrc) {
        this.lrc = lrc;
    }

    public KlyricBean getKlyric() {
        return klyric;
    }

    public void setKlyric(KlyricBean klyric) {
        this.klyric = klyric;
    }

    public TlyricBean getTlyric() {
        return tlyric;
    }

    public void setTlyric(TlyricBean tlyric) {
        this.tlyric = tlyric;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public static class TransUserBean {
        /**
         * id : 28391863
         * status : 99
         * demand : 1
         * userid : 35941609
         * nickname : 希Home-0869
         * uptime : 1459221362887
         */

        private int id;
        private int status;
        private int demand;
        private int userid;
        private String nickname;
        private long uptime;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public int getDemand() {
            return demand;
        }

        public void setDemand(int demand) {
            this.demand = demand;
        }

        public int getUserid() {
            return userid;
        }

        public void setUserid(int userid) {
            this.userid = userid;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public long getUptime() {
            return uptime;
        }

        public void setUptime(long uptime) {
            this.uptime = uptime;
        }
    }

    public static class LyricUserBean {
        /**
         * id : 28391863
         * status : 99
         * demand : 0
         * userid : 34214008
         * nickname : 不林塔
         * uptime : 1445787345526
         */

        private int id;
        private int status;
        private int demand;
        private int userid;
        private String nickname;
        private long uptime;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public int getDemand() {
            return demand;
        }

        public void setDemand(int demand) {
            this.demand = demand;
        }

        public int getUserid() {
            return userid;
        }

        public void setUserid(int userid) {
            this.userid = userid;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public long getUptime() {
            return uptime;
        }

        public void setUptime(long uptime) {
            this.uptime = uptime;
        }
    }

    public static class LrcBean {
        /**
         * version : 5
         * lyric : [00:00.000] 作曲 : 赤髪
         [00:01.000] 作词 : 赤髪
         [00:23.34]遠く 遠く 記憶の奥に沈めた
         [00:34.04]思い出を 掬い（すくい）上げる
         [00:44.88]拙い（つたない） 会話 慣れない姿に頬を染め
         [00:55.74]賑わう（にぎわう）方へ ゆっくりと歩くんだ
         [01:06.82]終わる夕暮れ 空を見上げ 近づく影
         [01:17.71]ほら ほら 手が触れ合って 気づけば
         [01:26.98]不器用に握った
         [01:33.71]高く空に 打ち上がり 咲いた
         [01:43.51]一瞬だけ キミを照らして 消えるんだ
         [01:56.33]嬉しそうにはしゃぐ横顔に
         [02:06.33]見惚れ焼きつく 記憶をぎゅっと
         [02:11.92]いつまでも 強く 強く この手握っていて
         [02:29.87]ひとつ ひとつ 描かれた歴史の欠片を
         [02:40.48]記憶へ沈め 抱きしめて 眠った
         [02:51.04]通り雨を 追いかけるように キミが現れ（あらわれ）
         [03:02.10]傘に入れてあげるよ 微笑み 頬を伝う雨
         [03:18.58]すべて世界がスローに写る（うつる）程
         [03:28.16]キミの言葉は 私への愛で満ちていて
         [03:39.95]抗う隙さえ与える間もなく
         [03:49.95]触れる息は 心もとなく 弱って
         [03:57.09]薄く 薄く キミが消えていく
         [04:06.74]キミが消えていく
         [04:12.59]流れる雨
         [04:22.74]止めどなく溢れ
         [04:41.55]土砂降りの雨はキミを通りぬけ
         [04:51.35]遠くの空は 嘘みたいに晴れていく
         [05:02.95]この世界がすべて書き換わり
         [05:13.10]キミの存在 記憶 すべて 消し去っても
         [05:20.70]消せない 消せない はしゃぐ笑顔
         [05:25.46]消せはしない キミを想っている
         [05:31.90]

         */

        private int version;
        private String lyric;

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getLyric() {
            return lyric;
        }

        public void setLyric(String lyric) {
            this.lyric = lyric;
        }
    }

    public static class KlyricBean {
        /**
         * version : 0
         * lyric : null
         */

        private int version;
        private String lyric;

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getLyric() {
            return lyric;
        }

        public void setLyric(String lyric) {
            this.lyric = lyric;
        }
    }

    public static class TlyricBean {
        /**
         * version : 2
         * lyric : [by:希Home-0869]
         [00:23.34]沉浸在遥远的记忆深处
         [00:34.04]从其中掬出一捧回忆
         [00:44.88]笨拙的交谈  因还未能习惯的姿态  双颊染上绯红
         [00:55.74]慢步走向那热闹的地方
         [01:06.82]抬头仰望夕阳   渐渐靠近的身影
         [01:17.71]你看  发现触碰到一起的双手
         [01:26.98]没志气的握紧
         [01:33.71]高空中升起盛开的烟火
         [01:43.51]瞬间照亮你的脸庞又转瞬消失不见
         [01:56.33]看上去很开心欢喜的你的侧脸
         [02:06.33]恍惚的注视着你  将这记忆铭记于心
         [02:11.92]无论何时都会紧紧握住这双手
         [02:29.87]一片一片     将描绘往昔的断片
         [02:40.48]沉入记忆  拥紧而眠
         [02:51.04]想要追赶阵雨时  你悄然出现
         [03:02.10]我来给你撑伞吧   雨水沿着笑脸低落
         [03:18.58]整个世界仿佛都缓慢下来
         [03:28.16]你的话语中充满了对我的爱
         [03:39.95]连反抗的时间都不曾给过我
         [03:49.95]相触的气息  心跳微弱
         [03:57.09]逐渐稀薄   你就这样消失
         [04:06.74]你就这样消失
         [04:12.59]流淌的雨水
         [04:22.74]不停的流下
         [04:41.55]倾盆大雨淋透你的身体
         [04:51.35]遥远的天空  不真实的开始放晴
         [05:02.95]这个世界已经重新书写
         [05:13.10]即使你的存在 你的记忆 全部消逝
         [05:20.70]欢笑的笑颜  永远不会消失
         [05:25.46]对你的想念  永远不会消失

         */

        private int version;
        private String lyric;

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getLyric() {
            return lyric;
        }

        public void setLyric(String lyric) {
            this.lyric = lyric;
        }
    }
}
