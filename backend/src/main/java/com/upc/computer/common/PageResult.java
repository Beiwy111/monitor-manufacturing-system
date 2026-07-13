package com.upc.computer.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页查询返回结果
 */
public class PageResult<T> {

    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> list;

    public PageResult() {
    }

    public PageResult(long total, int pageNum, int pageSize, List<T> list) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list;
    }

    public static <T> PageResult<T> of(long total, PageRequest pageRequest, List<T> list) {
        return new PageResult<>(total, pageRequest.getPageNum(), pageRequest.getPageSize(), list);
    }

    public static <T> PageResult<T> empty(PageRequest pageRequest) {
        return new PageResult<>(0, pageRequest.getPageNum(), pageRequest.getPageSize(), new ArrayList<T>());
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

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

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
