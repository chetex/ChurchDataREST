package com.chetex.church.rest.dto;

/**
 * DTO to represent a news or article item.
 */
public class NewsItemDTO {
    private String title;
    private String subtitle;
    private String imageUrl;
    private String link;
    private String excerpt;

    public NewsItemDTO() {
    }

    public NewsItemDTO(String title, String subtitle, String imageUrl, String link, String excerpt) {
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.link = link;
        this.excerpt = excerpt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }
}
