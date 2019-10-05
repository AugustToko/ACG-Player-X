package top.geek_studio.chenlongcould.musicplayer.model;

import java.util.List;

/**
 * For https://api.crypto-studio.com
 *
 * @author : chenlongcould
 * @date : 2019/10/04/16
 */
public class NetSong {

    /**
     * songs : [{"name":"海阔天空","id":347230,"pst":0,"t":0,"ar":[{"id":11127,"name":"Beyond","tns":[],"alias":[]}],"alia":[],"pop":100,"st":0,"rt":"600902000004240302","fee":8,"v":31,"crbt":null,"cf":"","al":{"id":34209,"name":"海阔天空","picUrl":"https://p1.music.126.net/QHw-RuMwfQkmgtiyRpGs0Q==/102254581395219.jpg","tns":[],"pic":102254581395219},"dt":326348,"h":{"br":320000,"fid":0,"size":13070578,"vd":0.109906},"m":{"br":160000,"fid":0,"size":6549371,"vd":0.272218},"l":{"br":96000,"fid":0,"size":3940469,"vd":0.228837},"a":null,"cd":"1","no":1,"rtUrl":null,"ftype":0,"rtUrls":[],"djId":0,"copyright":1,"s_id":0,"mark":0,"mv":376199,"rtype":0,"rurl":null,"mst":9,"cp":7002,"publishTime":746812800000},{"name":"Crown of the Loser","id":347231,"pst":0,"t":0,"ar":[{"id":11171,"name":"Beyond Cure","tns":[],"alias":[]}],"alia":[],"pop":5,"st":0,"rt":"","fee":8,"v":13,"crbt":null,"cf":"","al":{"id":34210,"name":"Your Head Smells Good","picUrl":"https://p1.music.126.net/uOAROZ8Ia72yvcmfMIg_Uw==/125344325570003.jpg","tns":["你的头闻起来好香"],"pic":125344325570003},"dt":162120,"h":{"br":320000,"fid":0,"size":6487814,"vd":-28400},"m":{"br":192000,"fid":0,"size":3892705,"vd":-26199},"l":{"br":128000,"fid":0,"size":2595151,"vd":-25000},"a":null,"cd":"1","no":2,"rtUrl":null,"ftype":0,"rtUrls":[],"djId":0,"copyright":2,"s_id":0,"mark":0,"mv":0,"rtype":0,"rurl":null,"mst":9,"cp":1400821,"publishTime":1277481600000}]
     * privileges : [{"id":347230,"fee":0,"payed":0,"st":-100,"pl":0,"dl":0,"sp":7,"cp":1,"subp":1,"cs":false,"maxbr":999000,"fl":0,"toast":false,"flag":256,"preSell":false},{"id":347231,"fee":0,"payed":0,"st":-100,"pl":0,"dl":0,"sp":7,"cp":1,"subp":1,"cs":false,"maxbr":999000,"fl":0,"toast":false,"flag":256,"preSell":false}]
     * code : 200
     */

    private long code;
    private List<SongsBean> songs;
    private List<PrivilegesBean> privileges;

    public long getCode() {
        return code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public List<SongsBean> getSongs() {
        return songs;
    }

    public void setSongs(List<SongsBean> songs) {
        this.songs = songs;
    }

    public List<PrivilegesBean> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(List<PrivilegesBean> privileges) {
        this.privileges = privileges;
    }

    public static class SongsBean {
        /**
         * name : 海阔天空
         * id : 347230
         * pst : 0
         * t : 0
         * ar : [{"id":11127,"name":"Beyond","tns":[],"alias":[]}]
         * alia : []
         * pop : 100
         * st : 0
         * rt : 600902000004240302
         * fee : 8
         * v : 31
         * crbt : null
         * cf :
         * al : {"id":34209,"name":"海阔天空","picUrl":"https://p1.music.126.net/QHw-RuMwfQkmgtiyRpGs0Q==/102254581395219.jpg","tns":[],"pic":102254581395219}
         * dt : 326348
         * h : {"br":320000,"fid":0,"size":13070578,"vd":0.109906}
         * m : {"br":160000,"fid":0,"size":6549371,"vd":0.272218}
         * l : {"br":96000,"fid":0,"size":3940469,"vd":0.228837}
         * a : null
         * cd : 1
         * no : 1
         * rtUrl : null
         * ftype : 0
         * rtUrls : []
         * djId : 0
         * copyright : 1
         * s_id : 0
         * mark : 0
         * mv : 376199
         * rtype : 0
         * rurl : null
         * mst : 9
         * cp : 7002
         * publishTime : 746812800000
         */

        private String name;
        private long id;
        private long pst;
        private long t;
        private long pop;
        private long st;
        private String rt;
        private long fee;
        private long v;
        private Object crbt;
        private String cf;
        private AlBean al;
        private long dt;
        private HBean h;
        private MBean m;
        private LBean l;
        private Object a;
        private String cd;
        private long no;
        private Object rtUrl;
        private long ftype;
        private long djId;
        private long copyright;
        private long s_id;
        private long mark;
        private long mv;
        private long rtype;
        private Object rurl;
        private long mst;
        private long cp;
        private long publishTime;
        private List<ArBean> ar;
        private List<?> alia;
        private List<?> rtUrls;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getPst() {
            return pst;
        }

        public void setPst(long pst) {
            this.pst = pst;
        }

        public long getT() {
            return t;
        }

        public void setT(long t) {
            this.t = t;
        }

        public long getPop() {
            return pop;
        }

        public void setPop(long pop) {
            this.pop = pop;
        }

        public long getSt() {
            return st;
        }

        public void setSt(long st) {
            this.st = st;
        }

        public String getRt() {
            return rt;
        }

        public void setRt(String rt) {
            this.rt = rt;
        }

        public long getFee() {
            return fee;
        }

        public void setFee(long fee) {
            this.fee = fee;
        }

        public long getV() {
            return v;
        }

        public void setV(long v) {
            this.v = v;
        }

        public Object getCrbt() {
            return crbt;
        }

        public void setCrbt(Object crbt) {
            this.crbt = crbt;
        }

        public String getCf() {
            return cf;
        }

        public void setCf(String cf) {
            this.cf = cf;
        }

        public AlBean getAl() {
            return al;
        }

        public void setAl(AlBean al) {
            this.al = al;
        }

        public long getDt() {
            return dt;
        }

        public void setDt(long dt) {
            this.dt = dt;
        }

        public HBean getH() {
            return h;
        }

        public void setH(HBean h) {
            this.h = h;
        }

        public MBean getM() {
            return m;
        }

        public void setM(MBean m) {
            this.m = m;
        }

        public LBean getL() {
            return l;
        }

        public void setL(LBean l) {
            this.l = l;
        }

        public Object getA() {
            return a;
        }

        public void setA(Object a) {
            this.a = a;
        }

        public String getCd() {
            return cd;
        }

        public void setCd(String cd) {
            this.cd = cd;
        }

        public long getNo() {
            return no;
        }

        public void setNo(long no) {
            this.no = no;
        }

        public Object getRtUrl() {
            return rtUrl;
        }

        public void setRtUrl(Object rtUrl) {
            this.rtUrl = rtUrl;
        }

        public long getFtype() {
            return ftype;
        }

        public void setFtype(long ftype) {
            this.ftype = ftype;
        }

        public long getDjId() {
            return djId;
        }

        public void setDjId(long djId) {
            this.djId = djId;
        }

        public long getCopyright() {
            return copyright;
        }

        public void setCopyright(long copyright) {
            this.copyright = copyright;
        }

        public long getS_id() {
            return s_id;
        }

        public void setS_id(long s_id) {
            this.s_id = s_id;
        }

        public long getMark() {
            return mark;
        }

        public void setMark(long mark) {
            this.mark = mark;
        }

        public long getMv() {
            return mv;
        }

        public void setMv(long mv) {
            this.mv = mv;
        }

        public long getRtype() {
            return rtype;
        }

        public void setRtype(long rtype) {
            this.rtype = rtype;
        }

        public Object getRurl() {
            return rurl;
        }

        public void setRurl(Object rurl) {
            this.rurl = rurl;
        }

        public long getMst() {
            return mst;
        }

        public void setMst(long mst) {
            this.mst = mst;
        }

        public long getCp() {
            return cp;
        }

        public void setCp(long cp) {
            this.cp = cp;
        }

        public long getPublishTime() {
            return publishTime;
        }

        public void setPublishTime(long publishTime) {
            this.publishTime = publishTime;
        }

        public List<ArBean> getAr() {
            return ar;
        }

        public void setAr(List<ArBean> ar) {
            this.ar = ar;
        }

        public List<?> getAlia() {
            return alia;
        }

        public void setAlia(List<?> alia) {
            this.alia = alia;
        }

        public List<?> getRtUrls() {
            return rtUrls;
        }

        public void setRtUrls(List<?> rtUrls) {
            this.rtUrls = rtUrls;
        }

        public static class AlBean {
            /**
             * id : 34209
             * name : 海阔天空
             * picUrl : https://p1.music.126.net/QHw-RuMwfQkmgtiyRpGs0Q==/102254581395219.jpg
             * tns : []
             * pic : 102254581395219
             */

            private long id;
            private String name;
            private String picUrl;
            private long pic;
            private List<?> tns;

            public long getId() {
                return id;
            }

            public void setId(long id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getPicUrl() {
                return picUrl;
            }

            public void setPicUrl(String picUrl) {
                this.picUrl = picUrl;
            }

            public long getPic() {
                return pic;
            }

            public void setPic(long pic) {
                this.pic = pic;
            }

            public List<?> getTns() {
                return tns;
            }

            public void setTns(List<?> tns) {
                this.tns = tns;
            }
        }

        public static class HBean {
            /**
             * br : 320000
             * fid : 0
             * size : 13070578
             * vd : 0.109906
             */

            private long br;
            private long fid;
            private long size;
            private double vd;

            public long getBr() {
                return br;
            }

            public void setBr(long br) {
                this.br = br;
            }

            public long getFid() {
                return fid;
            }

            public void setFid(long fid) {
                this.fid = fid;
            }

            public long getSize() {
                return size;
            }

            public void setSize(long size) {
                this.size = size;
            }

            public double getVd() {
                return vd;
            }

            public void setVd(double vd) {
                this.vd = vd;
            }
        }

        public static class MBean {
            /**
             * br : 160000
             * fid : 0
             * size : 6549371
             * vd : 0.272218
             */

            private long br;
            private long fid;
            private long size;
            private double vd;

            public long getBr() {
                return br;
            }

            public void setBr(long br) {
                this.br = br;
            }

            public long getFid() {
                return fid;
            }

            public void setFid(long fid) {
                this.fid = fid;
            }

            public long getSize() {
                return size;
            }

            public void setSize(long size) {
                this.size = size;
            }

            public double getVd() {
                return vd;
            }

            public void setVd(double vd) {
                this.vd = vd;
            }
        }

        public static class LBean {
            /**
             * br : 96000
             * fid : 0
             * size : 3940469
             * vd : 0.228837
             */

            private long br;
            private long fid;
            private long size;
            private double vd;

            public long getBr() {
                return br;
            }

            public void setBr(long br) {
                this.br = br;
            }

            public long getFid() {
                return fid;
            }

            public void setFid(long fid) {
                this.fid = fid;
            }

            public long getSize() {
                return size;
            }

            public void setSize(long size) {
                this.size = size;
            }

            public double getVd() {
                return vd;
            }

            public void setVd(double vd) {
                this.vd = vd;
            }
        }

        public static class ArBean {
            /**
             * id : 11127
             * name : Beyond
             * tns : []
             * alias : []
             */

            private long id;
            private String name;
            private List<?> tns;
            private List<?> alias;

            public long getId() {
                return id;
            }

            public void setId(long id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public List<?> getTns() {
                return tns;
            }

            public void setTns(List<?> tns) {
                this.tns = tns;
            }

            public List<?> getAlias() {
                return alias;
            }

            public void setAlias(List<?> alias) {
                this.alias = alias;
            }
        }
    }

    public static class PrivilegesBean {
        /**
         * id : 347230
         * fee : 0
         * payed : 0
         * st : -100
         * pl : 0
         * dl : 0
         * sp : 7
         * cp : 1
         * subp : 1
         * cs : false
         * maxbr : 999000
         * fl : 0
         * toast : false
         * flag : 256
         * preSell : false
         */

        private long id;
        private long fee;
        private long payed;
        private long st;
        private long pl;
        private long dl;
        private long sp;
        private long cp;
        private long subp;
        private boolean cs;
        private long maxbr;
        private long fl;
        private boolean toast;
        private long flag;
        private boolean preSell;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getFee() {
            return fee;
        }

        public void setFee(long fee) {
            this.fee = fee;
        }

        public long getPayed() {
            return payed;
        }

        public void setPayed(long payed) {
            this.payed = payed;
        }

        public long getSt() {
            return st;
        }

        public void setSt(long st) {
            this.st = st;
        }

        public long getPl() {
            return pl;
        }

        public void setPl(long pl) {
            this.pl = pl;
        }

        public long getDl() {
            return dl;
        }

        public void setDl(long dl) {
            this.dl = dl;
        }

        public long getSp() {
            return sp;
        }

        public void setSp(long sp) {
            this.sp = sp;
        }

        public long getCp() {
            return cp;
        }

        public void setCp(long cp) {
            this.cp = cp;
        }

        public long getSubp() {
            return subp;
        }

        public void setSubp(long subp) {
            this.subp = subp;
        }

        public boolean isCs() {
            return cs;
        }

        public void setCs(boolean cs) {
            this.cs = cs;
        }

        public long getMaxbr() {
            return maxbr;
        }

        public void setMaxbr(long maxbr) {
            this.maxbr = maxbr;
        }

        public long getFl() {
            return fl;
        }

        public void setFl(long fl) {
            this.fl = fl;
        }

        public boolean isToast() {
            return toast;
        }

        public void setToast(boolean toast) {
            this.toast = toast;
        }

        public long getFlag() {
            return flag;
        }

        public void setFlag(long flag) {
            this.flag = flag;
        }

        public boolean isPreSell() {
            return preSell;
        }

        public void setPreSell(boolean preSell) {
            this.preSell = preSell;
        }
    }
}
