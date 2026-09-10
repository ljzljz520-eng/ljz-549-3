package com.example.labkey.model;

/**
 * 申请状态。
 * PENDING  待审核
 * APPROVED 已通过
 * REJECTED 已驳回
 */
public enum ApplyStatus {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已驳回");

    private final String text;

    ApplyStatus(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
