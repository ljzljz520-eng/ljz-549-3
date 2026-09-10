package com.example.labkey.model;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 一条钥匙申请记录。
 */
public class LabKeyApplication {

    private final String applyNo;
    private final String name;
    private final String studentNo;
    private final String lab;
    private final String useTime;
    private final String reason;
    private final long createdAt;
    private final AtomicReference<ApplyStatus> status = new AtomicReference<>(ApplyStatus.PENDING);

    public LabKeyApplication(String applyNo, String name, String studentNo,
                             String lab, String useTime, String reason, long createdAt) {
        this.applyNo = applyNo;
        this.name = name;
        this.studentNo = studentNo;
        this.lab = lab;
        this.useTime = useTime;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public String getApplyNo() {
        return applyNo;
    }

    public String getName() {
        return name;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public String getLab() {
        return lab;
    }

    public String getUseTime() {
        return useTime;
    }

    public String getReason() {
        return reason;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public ApplyStatus getStatus() {
        return status.get();
    }

    public void setStatus(ApplyStatus next) {
        this.status.set(next);
    }
}
