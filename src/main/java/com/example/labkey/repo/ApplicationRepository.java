package com.example.labkey.repo;

import com.example.labkey.model.ApplyStatus;
import com.example.labkey.model.LabKeyApplication;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 内存仓库（单例）。重启后数据清空，适合演示；生产应替换为数据库实现。
 *
 * <p>为了让状态查询接口可演示，申请提交 20 秒后自动变为「已通过」；
 * 若学号末位为 4，则 15 秒后变为「已驳回」。</p>
 */
public final class ApplicationRepository {

    private static final ApplicationRepository INSTANCE = new ApplicationRepository();

    private final ConcurrentHashMap<String, LabKeyApplication> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "labkey-status-scheduler");
                t.setDaemon(true);
                return t;
            });

    private ApplicationRepository() {
    }

    public static ApplicationRepository getInstance() {
        return INSTANCE;
    }

    /**
     * 保存一条申请，并调度其状态流转。
     */
    public LabKeyApplication save(LabKeyApplication application) {
        store.put(application.getApplyNo(), application);

        boolean willReject = application.getStudentNo()
                .charAt(application.getStudentNo().length() - 1) == '4';
        long delaySeconds = willReject ? 15 : 20;
        final ApplyStatus target = willReject ? ApplyStatus.REJECTED : ApplyStatus.APPROVED;

        final String applyNo = application.getApplyNo();
        scheduler.schedule(() -> {
            LabKeyApplication a = store.get(applyNo);
            if (a != null && a.getStatus() == ApplyStatus.PENDING) {
                a.setStatus(target);
            }
        }, delaySeconds, TimeUnit.SECONDS);

        return application;
    }

    public LabKeyApplication find(String applyNo) {
        if (applyNo == null) {
            return null;
        }
        return store.get(applyNo.trim());
    }

    /**
     * 生成申请编号：LK + yyyyMMdd + 6 位数字（如 LK20260910-483920）。
     */
    public String nextApplyNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 20; i++) {
            String candidate = "LK" + datePart + "-" + String.format("%06d", random.nextInt(1_000_000));
            if (!store.containsKey(candidate)) {
                return candidate;
            }
        }
        // 极端碰撞情况下退化为纳秒随机
        long suffix = Math.abs(System.nanoTime()) % 1_000_000;
        return "LK" + datePart + "-" + String.format("%06d", suffix);
    }
}
