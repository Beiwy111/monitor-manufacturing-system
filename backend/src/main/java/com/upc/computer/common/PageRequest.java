package com.upc.computer.common;

/**
 * 分页查询请求参数
 */
public class PageRequest {

    /** 当前页码，从1开始 */
    private int pageNum = 1;

    /** 每页条数 */
    private int pageSize = 10;

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /** 计算 SQL offset */
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
