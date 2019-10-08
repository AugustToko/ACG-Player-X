package top.geek_studio.chenlongcould.musicplayer.model.yuepic;

import java.io.Serializable;
import java.util.List;

/**
 * @author : chenlongcould
 * @date : 2019/10/07/19
 */
public class YuePic implements Serializable {

    private static final long serialVersionUID = -7815738517333462426L;

    /**
     * id : dZQIW071ug0
     * created_at : 2019-10-02T22:37:07-04:00
     * updated_at : 2019-10-07T01:24:17-04:00
     * width : 3648
     * height : 5472
     * color : #242515
     * description : nullzss
     * alt_description : closed door
     * urls : {"raw":"https://images.unsplash.com/photo-1570070067144-64f52fe8f185?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjk0ODc1fQ","full":"https://images.unsplash.com/photo-1570070067144-64f52fe8f185?ixlib=rb-1.2.1&q=85&fm=jpg&crop=entropy&cs=srgb&ixid=eyJhcHBfaWQiOjk0ODc1fQ","regular":"https://images.unsplash.com/photo-1570070067144-64f52fe8f185?ixlib=rb-1.2.1&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max&ixid=eyJhcHBfaWQiOjk0ODc1fQ","small":"https://images.unsplash.com/photo-1570070067144-64f52fe8f185?ixlib=rb-1.2.1&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=400&fit=max&ixid=eyJhcHBfaWQiOjk0ODc1fQ","thumb":"https://images.unsplash.com/photo-1570070067144-64f52fe8f185?ixlib=rb-1.2.1&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=200&fit=max&ixid=eyJhcHBfaWQiOjk0ODc1fQ"}
     * links : {"self":"https://api.unsplash.com/photos/dZQIW071ug0","html":"https://unsplash.com/photos/dZQIW071ug0","download":"https://unsplash.com/photos/dZQIW071ug0/download","download_location":"https://api.unsplash.com/photos/dZQIW071ug0/download"}
     * categories : []
     * likes : 94
     * liked_by_user : false
     * current_user_collections : []
     * user : {"id":"d1ZO_uRBez4","updated_at":"2019-10-04T19:33:16-04:00","username":"millieao","name":"Millie Olsen","first_name":"Millie","last_name":"Olsen","twitter_username":null,"portfolio_url":"https://www.millieolsen.com","bio":"Wedding photographer located in Southern Utah, USA\r\n@millieolsenphotography","location":"UT, USA","links":{"self":"https://api.unsplash.com/users/millieao","html":"https://unsplash.com/@millieao","photos":"https://api.unsplash.com/users/millieao/photos","likes":"https://api.unsplash.com/users/millieao/likes","portfolio":"https://api.unsplash.com/users/millieao/portfolio","following":"https://api.unsplash.com/users/millieao/following","followers":"https://api.unsplash.com/users/millieao/followers"},"profile_image":{"small":"https://images.unsplash.com/profile-fb-1555376129-cc1df2c99fd7.jpg?ixlib=rb-1.2.1&q=80&fm=jpg&crop=faces&cs=tinysrgb&fit=crop&h=32&w=32","medium":"https://images.unsplash.com/profile-fb-1555376129-cc1df2c99fd7.jpg?ixlib=rb-1.2.1&q=80&fm=jpg&crop=faces&cs=tinysrgb&fit=crop&h=64&w=64","large":"https://images.unsplash.com/profile-fb-1555376129-cc1df2c99fd7.jpg?ixlib=rb-1.2.1&q=80&fm=jpg&crop=faces&cs=tinysrgb&fit=crop&h=128&w=128"},"instagram_username":"millieolsenphotography","total_collections":1,"total_likes":36,"total_photos":33,"accepted_tos":true}
     * exif : {"make":null,"model":null,"exposure_time":null,"aperture":null,"focal_length":null,"iso":null}
     * location : {"title":null,"name":null,"city":null,"country":null,"position":{"latitude":null,"longitude":null}}
     * views : 354650
     * downloads : 846
     */

    private String id;
    private String created_at;
    private String updated_at;
    private int width;
    private int height;
    private String color;
    private Object description;
    private String alt_description;
    private UrlsBean urls;
    private LinksBean links;
    private int likes;
    private boolean liked_by_user;
    private UserBean user;
    private ExifBean exif;
    private LocationBean location;
    private int views;
    private int downloads;
    private List<?> categories;
    private List<?> current_user_collections;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Object getDescription() {
        return description;
    }

    public void setDescription(Object description) {
        this.description = description;
    }

    public String getAlt_description() {
        return alt_description;
    }

    public void setAlt_description(String alt_description) {
        this.alt_description = alt_description;
    }

    public UrlsBean getUrls() {
        return urls;
    }

    public void setUrls(UrlsBean urls) {
        this.urls = urls;
    }

    public LinksBean getLinks() {
        return links;
    }

    public void setLinks(LinksBean links) {
        this.links = links;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public boolean isLiked_by_user() {
        return liked_by_user;
    }

    public void setLiked_by_user(boolean liked_by_user) {
        this.liked_by_user = liked_by_user;
    }

    public UserBean getUser() {
        return user;
    }

    public void setUser(UserBean user) {
        this.user = user;
    }

    public ExifBean getExif() {
        return exif;
    }

    public void setExif(ExifBean exif) {
        this.exif = exif;
    }

    public LocationBean getLocation() {
        return location;
    }

    public void setLocation(LocationBean location) {
        this.location = location;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getDownloads() {
        return downloads;
    }

    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }

    public List<?> getCategories() {
        return categories;
    }

    public void setCategories(List<?> categories) {
        this.categories = categories;
    }

    public List<?> getCurrent_user_collections() {
        return current_user_collections;
    }

    public void setCurrent_user_collections(List<?> current_user_collections) {
        this.current_user_collections = current_user_collections;
    }

    public static class UrlsBean implements Serializable {

        private static final long serialVersionUID = -7815735617333468526L;

        /**
         * raw : https://images.unsplash.com/photo-1570070067144-64f52fe8f185?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjk0ODc1fQ
         * full : https://images.unsplash.com/photo-1570070067144-64f52fe8f185?ixlib=rb-1.2.1&q=85&fm=jpg&crop=entropy&cs=srgb&ixid=eyJhcHBfaWQiOjk0ODc1fQ
         * regular : https://images.unsplash.com/photo-1570070067144-64f52fe8f185?ixlib=rb-1.2.1&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max&ixid=eyJhcHBfaWQiOjk0ODc1fQ
         * small : https://images.unsplash.com/photo-1570070067144-64f52fe8f185?ixlib=rb-1.2.1&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=400&fit=max&ixid=eyJhcHBfaWQiOjk0ODc1fQ
         * thumb : https://images.unsplash.com/photo-1570070067144-64f52fe8f185?ixlib=rb-1.2.1&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=200&fit=max&ixid=eyJhcHBfaWQiOjk0ODc1fQ
         */

        private String raw;
        private String full;
        private String regular;
        private String small;
        private String thumb;

        public String getRaw() {
            return raw;
        }

        public void setRaw(String raw) {
            this.raw = raw;
        }

        public String getFull() {
            return full;
        }

        public void setFull(String full) {
            this.full = full;
        }

        public String getRegular() {
            return regular;
        }

        public void setRegular(String regular) {
            this.regular = regular;
        }

        public String getSmall() {
            return small;
        }

        public void setSmall(String small) {
            this.small = small;
        }

        public String getThumb() {
            return thumb;
        }

        public void setThumb(String thumb) {
            this.thumb = thumb;
        }
    }

    public static class LinksBean implements Serializable {

        private static final long serialVersionUID = -7816374617333468526L;

        /**
         * self : https://api.unsplash.com/photos/dZQIW071ug0
         * html : https://unsplash.com/photos/dZQIW071ug0
         * download : https://unsplash.com/photos/dZQIW071ug0/download
         * download_location : https://api.unsplash.com/photos/dZQIW071ug0/download
         */

        private String self;
        private String html;
        private String download;
        private String download_location;

        public String getSelf() {
            return self;
        }

        public void setSelf(String self) {
            this.self = self;
        }

        public String getHtml() {
            return html;
        }

        public void setHtml(String html) {
            this.html = html;
        }

        public String getDownload() {
            return download;
        }

        public void setDownload(String download) {
            this.download = download;
        }

        public String getDownload_location() {
            return download_location;
        }

        public void setDownload_location(String download_location) {
            this.download_location = download_location;
        }
    }

    public static class UserBean implements Serializable {

        private static final long serialVersionUID = -7815712385333468526L;

        /**
         * id : d1ZO_uRBez4
         * updated_at : 2019-10-04T19:33:16-04:00
         * username : millieao
         * name : Millie Olsen
         * first_name : Millie
         * last_name : Olsen
         * twitter_username : null
         * portfolio_url : https://www.millieolsen.com
         * bio : Wedding photographer located in Southern Utah, USA
         *
         * @millieolsenphotography location : UT, USA
         * links : {"self":"https://api.unsplash.com/users/millieao","html":"https://unsplash.com/@millieao","photos":"https://api.unsplash.com/users/millieao/photos","likes":"https://api.unsplash.com/users/millieao/likes","portfolio":"https://api.unsplash.com/users/millieao/portfolio","following":"https://api.unsplash.com/users/millieao/following","followers":"https://api.unsplash.com/users/millieao/followers"}
         * profile_image : {"small":"https://images.unsplash.com/profile-fb-1555376129-cc1df2c99fd7.jpg?ixlib=rb-1.2.1&q=80&fm=jpg&crop=faces&cs=tinysrgb&fit=crop&h=32&w=32","medium":"https://images.unsplash.com/profile-fb-1555376129-cc1df2c99fd7.jpg?ixlib=rb-1.2.1&q=80&fm=jpg&crop=faces&cs=tinysrgb&fit=crop&h=64&w=64","large":"https://images.unsplash.com/profile-fb-1555376129-cc1df2c99fd7.jpg?ixlib=rb-1.2.1&q=80&fm=jpg&crop=faces&cs=tinysrgb&fit=crop&h=128&w=128"}
         * instagram_username : millieolsenphotography
         * total_collections : 1
         * total_likes : 36
         * total_photos : 33
         * accepted_tos : true
         */

        private String id;
        private String updated_at;
        private String username;
        private String name;
        private String first_name;
        private String last_name;
        private Object twitter_username;
        private String portfolio_url;
        private String bio;
        private String location;
        private LinksBeanX links;
        private ProfileImageBean profile_image;
        private String instagram_username;
        private int total_collections;
        private int total_likes;
        private int total_photos;
        private boolean accepted_tos;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUpdated_at() {
            return updated_at;
        }

        public void setUpdated_at(String updated_at) {
            this.updated_at = updated_at;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFirst_name() {
            return first_name;
        }

        public void setFirst_name(String first_name) {
            this.first_name = first_name;
        }

        public String getLast_name() {
            return last_name;
        }

        public void setLast_name(String last_name) {
            this.last_name = last_name;
        }

        public Object getTwitter_username() {
            return twitter_username;
        }

        public void setTwitter_username(Object twitter_username) {
            this.twitter_username = twitter_username;
        }

        public String getPortfolio_url() {
            return portfolio_url;
        }

        public void setPortfolio_url(String portfolio_url) {
            this.portfolio_url = portfolio_url;
        }

        public String getBio() {
            return bio;
        }

        public void setBio(String bio) {
            this.bio = bio;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public LinksBeanX getLinks() {
            return links;
        }

        public void setLinks(LinksBeanX links) {
            this.links = links;
        }

        public ProfileImageBean getProfile_image() {
            return profile_image;
        }

        public void setProfile_image(ProfileImageBean profile_image) {
            this.profile_image = profile_image;
        }

        public String getInstagram_username() {
            return instagram_username;
        }

        public void setInstagram_username(String instagram_username) {
            this.instagram_username = instagram_username;
        }

        public int getTotal_collections() {
            return total_collections;
        }

        public void setTotal_collections(int total_collections) {
            this.total_collections = total_collections;
        }

        public int getTotal_likes() {
            return total_likes;
        }

        public void setTotal_likes(int total_likes) {
            this.total_likes = total_likes;
        }

        public int getTotal_photos() {
            return total_photos;
        }

        public void setTotal_photos(int total_photos) {
            this.total_photos = total_photos;
        }

        public boolean isAccepted_tos() {
            return accepted_tos;
        }

        public void setAccepted_tos(boolean accepted_tos) {
            this.accepted_tos = accepted_tos;
        }

        public static class LinksBeanX implements Serializable {

            private static final long serialVersionUID = -7815735617312578526L;

            /**
             * self : https://api.unsplash.com/users/millieao
             * html : https://unsplash.com/@millieao
             * photos : https://api.unsplash.com/users/millieao/photos
             * likes : https://api.unsplash.com/users/millieao/likes
             * portfolio : https://api.unsplash.com/users/millieao/portfolio
             * following : https://api.unsplash.com/users/millieao/following
             * followers : https://api.unsplash.com/users/millieao/followers
             */

            private String self;
            private String html;
            private String photos;
            private String likes;
            private String portfolio;
            private String following;
            private String followers;

            public String getSelf() {
                return self;
            }

            public void setSelf(String self) {
                this.self = self;
            }

            public String getHtml() {
                return html;
            }

            public void setHtml(String html) {
                this.html = html;
            }

            public String getPhotos() {
                return photos;
            }

            public void setPhotos(String photos) {
                this.photos = photos;
            }

            public String getLikes() {
                return likes;
            }

            public void setLikes(String likes) {
                this.likes = likes;
            }

            public String getPortfolio() {
                return portfolio;
            }

            public void setPortfolio(String portfolio) {
                this.portfolio = portfolio;
            }

            public String getFollowing() {
                return following;
            }

            public void setFollowing(String following) {
                this.following = following;
            }

            public String getFollowers() {
                return followers;
            }

            public void setFollowers(String followers) {
                this.followers = followers;
            }
        }

        public static class ProfileImageBean implements Serializable {

            private static final long serialVersionUID = -1815735617333468526L;

            /**
             * small : https://images.unsplash.com/profile-fb-1555376129-cc1df2c99fd7.jpg?ixlib=rb-1.2.1&q=80&fm=jpg&crop=faces&cs=tinysrgb&fit=crop&h=32&w=32
             * medium : https://images.unsplash.com/profile-fb-1555376129-cc1df2c99fd7.jpg?ixlib=rb-1.2.1&q=80&fm=jpg&crop=faces&cs=tinysrgb&fit=crop&h=64&w=64
             * large : https://images.unsplash.com/profile-fb-1555376129-cc1df2c99fd7.jpg?ixlib=rb-1.2.1&q=80&fm=jpg&crop=faces&cs=tinysrgb&fit=crop&h=128&w=128
             */

            private String small;
            private String medium;
            private String large;

            public String getSmall() {
                return small;
            }

            public void setSmall(String small) {
                this.small = small;
            }

            public String getMedium() {
                return medium;
            }

            public void setMedium(String medium) {
                this.medium = medium;
            }

            public String getLarge() {
                return large;
            }

            public void setLarge(String large) {
                this.large = large;
            }
        }
    }

    public static class ExifBean implements Serializable {

        private static final long serialVersionUID = -6515735617333469856L;

        /**
         * make : null
         * model : null
         * exposure_time : null
         * aperture : null
         * focal_length : null
         * iso : null
         */

        private Object make;
        private Object model;
        private Object exposure_time;
        private Object aperture;
        private Object focal_length;
        private Object iso;

        public Object getMake() {
            return make;
        }

        public void setMake(Object make) {
            this.make = make;
        }

        public Object getModel() {
            return model;
        }

        public void setModel(Object model) {
            this.model = model;
        }

        public Object getExposure_time() {
            return exposure_time;
        }

        public void setExposure_time(Object exposure_time) {
            this.exposure_time = exposure_time;
        }

        public Object getAperture() {
            return aperture;
        }

        public void setAperture(Object aperture) {
            this.aperture = aperture;
        }

        public Object getFocal_length() {
            return focal_length;
        }

        public void setFocal_length(Object focal_length) {
            this.focal_length = focal_length;
        }

        public Object getIso() {
            return iso;
        }

        public void setIso(Object iso) {
            this.iso = iso;
        }
    }

    public static class LocationBean implements Serializable {

        private static final long serialVersionUID = -7715735689633468526L;

        /**
         * title : null
         * name : null
         * city : null
         * country : null
         * position : {"latitude":null,"longitude":null}
         */

        private Object title;
        private Object name;
        private Object city;
        private Object country;
        private PositionBean position;

        public Object getTitle() {
            return title;
        }

        public void setTitle(Object title) {
            this.title = title;
        }

        public Object getName() {
            return name;
        }

        public void setName(Object name) {
            this.name = name;
        }

        public Object getCity() {
            return city;
        }

        public void setCity(Object city) {
            this.city = city;
        }

        public Object getCountry() {
            return country;
        }

        public void setCountry(Object country) {
            this.country = country;
        }

        public PositionBean getPosition() {
            return position;
        }

        public void setPosition(PositionBean position) {
            this.position = position;
        }

        public static class PositionBean implements Serializable {

            private static final long serialVersionUID = -7815735617338888526L;

            /**
             * latitude : null
             * longitude : null
             */

            private Object latitude;
            private Object longitude;

            public Object getLatitude() {
                return latitude;
            }

            public void setLatitude(Object latitude) {
                this.latitude = latitude;
            }

            public Object getLongitude() {
                return longitude;
            }

            public void setLongitude(Object longitude) {
                this.longitude = longitude;
            }
        }
    }
}
