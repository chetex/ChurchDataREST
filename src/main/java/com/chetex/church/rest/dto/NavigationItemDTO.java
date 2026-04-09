package com.chetex.church.rest.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO to represent a navigation element (menu or submenu).
 */
public class NavigationItemDTO {
    private String name;
    private String url;
    private List<NavigationItemDTO> subItems;

    public NavigationItemDTO() {
        this.subItems = new ArrayList<>();
    }

    public NavigationItemDTO(String name, String url) {
        this.name = name;
        this.url = url;
        this.subItems = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<NavigationItemDTO> getSubItems() {
        return subItems;
    }

    public void setSubItems(List<NavigationItemDTO> subItems) {
        this.subItems = subItems;
    }

    public void addSubItem(NavigationItemDTO item) {
        this.subItems.add(item);
    }
}
