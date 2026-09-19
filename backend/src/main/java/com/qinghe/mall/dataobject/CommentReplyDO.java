package com.qinghe.mall.dataobject;

import java.util.Date;

/**
 * 评价回复（A4，v1.5）：商家对本店商品评价的一对一回复（uk_comment_id 防重复回复）。
 */
public class CommentReplyDO {

    private String id;
    private String commentId;
    private Long merchantId;
    private String content;
    private Date gmtCreated;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Date getGmtCreated() { return gmtCreated; }
    public void setGmtCreated(Date gmtCreated) { this.gmtCreated = gmtCreated; }
}
