package com.qinghe.mall.param;

public class BasePageParam {

    private Integer pagination = 1;
    private Integer pageSize = 10;

    public Integer getPagination() {
        return pagination;
    }

    public void setPagination(Integer pagination) {
        this.pagination = pagination;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
