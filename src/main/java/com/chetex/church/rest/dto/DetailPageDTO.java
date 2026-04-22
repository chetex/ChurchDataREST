package com.chetex.church.rest.dto;

/**
 * DTO to represent the detail view of a page or article.
 *
 * <p>Only the fields the mobile app actually consumes are persisted: title,
 * optional subtitle, main image, the plain-text content of the article and
 * the publication date as rendered on the site.</p>
 */
public class DetailPageDTO {
    private String title;      // Headline of the article.
    private String subtitle;   // Optional secondary line (meta/excerpt).
    private String imageUrl;   // Absolute URL of the featured image.
    private String content;    // Plain-text body (paragraphs joined with blank lines).
    private String date;       // Publication date string as shown on the site (e.g. "junio 21, 2025").

    public DetailPageDTO() {
    }

    public DetailPageDTO(String title, String subtitle, String imageUrl, String content, String date) {
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.content = content;
        this.date = date;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
