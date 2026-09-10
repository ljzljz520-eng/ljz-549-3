package com.example.labkey.model;

/**
 * 申请提交请求体（JSON 反序列化对象）。
 */
public class ApplicationRequest {

    /** 姓名 */
    private String name;
    /** 学号 */
    private String studentNo;
    /** 实验室（中文名，必须在白名单内） */
    private String lab;
    /** 使用时间，ISO-8601，如 2026-09-12T14:30 */
    private String useTime;
    /** 申请理由（可选） */
    private String reason;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getLab() {
        return lab;
    }

    public void setLab(String lab) {
        this.lab = lab;
    }

    public String getUseTime() {
        return useTime;
    }

    public void setUseTime(String useTime) {
        this.useTime = useTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
