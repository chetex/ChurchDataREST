package com.chetex.church.rest.dto;

/**
 * DTO to represent the detail view of a page or article.
 */
public class DetailPageDTO {
    private String title;
    private String subtitle;
    private String imageUrl;
    private String fullContentHtml; // Full content including HTML to preserve formatting and links

    public DetailPageDTO() {
    }

    public DetailPageDTO(String title, String subtitle, String imageUrl, String fullContentHtml) {
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.fullContentHtml = fullContentHtml;
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

    public String getFullContentHtml() {
        return fullContentHtml;
    }

    public void setFullContentHtml(String fullContentHtml) {
        this.fullContentHtml = fullContentHtml;
    }
}
