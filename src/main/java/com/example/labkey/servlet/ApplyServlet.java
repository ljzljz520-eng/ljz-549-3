package com.example.labkey.servlet;

import com.example.labkey.model.ApplicationRequest;
import com.example.labkey.model.LabKeyApplication;
import com.example.labkey.repo.ApplicationRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.labkey.util.JsonUtil.errorBody;
import static com.example.labkey.util.JsonUtil.writeJson;

/**
 * 提交钥匙申请。
 *
 * <p>POST /api/applications，Content-Type: application/json; charset=UTF-8</p>
 * <pre>
 * 成功：HTTP 201 {"success":true,"applyNo":"LK20260910-483920", ...}
 * 校验失败：HTTP 400 {"code":400,"message":"提交信息有误，请检查标红字段","errors":{"name":"姓名长度需为 2~20 个字符", ...}}
 * </pre>
 */
@WebServlet(name = "applyServlet", urlPatterns = {"/api/applications"})
public class ApplyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** 允许申请的中文实验室白名单 */
    private static final List<String> ALLOWED_LABS = Arrays.asList(
            "人工智能实验室",
            "嵌入式系统实验室",
            "网络安全实验室",
            "数字媒体实验室",
            "集成电路实验室"
    );

    /** 使用时间允许的范围：不早于当前，不晚于 30 天后 */
    private static final long MAX_DAYS_AHEAD = 30;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");

        // 1. 校验 Content-Type
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            writeJson(resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    errorBody(415, "请求头 Content-Type 必须为 application/json", null));
            return;
        }

        // 2. 读取并解析 JSON
        ApplicationRequest body;
        try {
            String json = readBody(req);
            if (json.trim().isEmpty()) {
                writeJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                        errorBody(400, "请求体不能为空", null));
                return;
            }
            body = MAPPER.readValue(json, ApplicationRequest.class);
        } catch (IOException e) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    errorBody(400, "JSON 格式不合法，无法解析", null));
            return;
        }

        // 3. 逐字段校验，收集全部字段错误
        Map<String, String> errors = new LinkedHashMap<>();
        String name = body.getName() == null ? "" : body.getName().trim();
        String studentNo = body.getStudentNo() == null ? "" : body.getStudentNo().trim();
        String lab = body.getLab() == null ? "" : body.getLab().trim();
        String useTime = body.getUseTime() == null ? "" : body.getUseTime().trim();
        String reason = body.getReason() == null ? "" : body.getReason().trim();

        if (name.isEmpty()) {
            errors.put("name", "请填写姓名");
        } else if (name.length() < 2 || name.length() > 20) {
            errors.put("name", "姓名长度需为 2~20 个字符");
        }

        if (studentNo.isEmpty()) {
            errors.put("studentNo", "请填写学号");
        } else if (!studentNo.matches("\\d{8,12}")) {
            errors.put("studentNo", "学号需为 8~12 位数字");
        }

        if (lab.isEmpty()) {
            errors.put("lab", "请选择实验室");
        } else if (!ALLOWED_LABS.contains(lab)) {
            errors.put("lab", "所选实验室不在可申请范围内");
        }

        LocalDateTime parsedUseTime = null;
        if (useTime.isEmpty()) {
            errors.put("useTime", "请选择使用时间");
        } else {
            try {
                parsedUseTime = LocalDateTime.parse(useTime);
                LocalDateTime now = LocalDateTime.now();
                if (parsedUseTime.isAfter(now.plusDays(MAX_DAYS_AHEAD))) {
                    errors.put("useTime", "使用时间最远可预约 30 天内");
                } else if (parsedUseTime.isBefore(now)) {
                    errors.put("useTime", "使用时间不能早于当前时间");
                }
            } catch (DateTimeParseException e) {
                errors.put("useTime", "时间格式不正确，应为 yyyy-MM-ddTHH:mm");
            }
        }

        if (reason.length() > 200) {
            errors.put("reason", "申请理由不能超过 200 字");
        }

        if (!errors.isEmpty()) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    errorBody(400, "提交信息有误，请检查标红字段", errors));
            return;
        }

        // 4. 生成申请编号并保存
        ApplicationRepository repository = ApplicationRepository.getInstance();
        String applyNo = repository.nextApplyNo();
        LabKeyApplication application = new LabKeyApplication(
                applyNo, name, studentNo, lab,
                parsedUseTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                reason.isEmpty() ? null : reason,
                System.currentTimeMillis());
        repository.save(application);

        // 5. 返回成功
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("applyNo", applyNo);
        data.put("name", name);
        data.put("studentNo", studentNo);
        data.put("lab", lab);
        data.put("useTime", application.getUseTime());
        data.put("status", "PENDING");
        data.put("statusText", "待审核");
        data.put("message", "申请已提交，请妥善保管申请编号");
        writeJson(resp, HttpServletResponse.SC_CREATED, data);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            resp.setHeader("Allow", "POST");
            writeJson(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    errorBody(405, "仅支持 POST 方法", null));
            return;
        }
        super.service(req, resp);
    }

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            char[] buf = new char[1024];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
        }
        return sb.toString();
    }
}
