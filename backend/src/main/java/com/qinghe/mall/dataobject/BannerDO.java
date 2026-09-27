package com.qinghe.mall.dataobject;

import java.util.Date;

/**
 * 首页运营位（D2，v1.7）：管理端配置的首页横幅位（标题/图片/跳转/排序/上下架）。
 */
public class BannerDO {

    private String id;
    private String title;
    private String image;
    private String linkUrl;
    private Integer sortOrder;
    private String status;
    private Date gmtCreated;
    private Date gmtModified;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getGmtCreated() { return gmtCreated; }
    public void setGmtCreated(Date gmtCreated) { this.gmtCreated = gmtCreated; }
    public Date getGmtModified() { return gmtModified; }
    public void setGmtModified(Date gmtModified) { this.gmtModified = gmtModified; }
}
