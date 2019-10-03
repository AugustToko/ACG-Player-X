package top.geek_studio.chenlongcould.musicplayer.model;

/**
 * @author : chenlongcould
 * @date : 2019/10/03/20
 */
public class Hitokoto {

    /**
     * id : 4432
     * hitokoto : 真正的恐怖，存在于约定俗成的恐怖之外。
     * type : c
     * from : 东方永夜抄 ～ Imperishable Night.
     * creator : rvalue
     * created_at : 1557547691
     */

    private int id;
    private String hitokoto;
    private String type;
    private String from;
    private String creator;
    private String created_at;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHitokoto() {
        return hitokoto;
    }

    public void setHitokoto(String hitokoto) {
        this.hitokoto = hitokoto;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
}
