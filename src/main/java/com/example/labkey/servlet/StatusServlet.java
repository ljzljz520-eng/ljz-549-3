package com.example.labkey.servlet;

import com.example.labkey.model.LabKeyApplication;
import com.example.labkey.repo.ApplicationRepository;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.example.labkey.util.JsonUtil.errorBody;
import static com.example.labkey.util.JsonUtil.writeJson;

/**
 * 查询申请状态。
 *
 * <p>GET /api/applications/status?applyNo=LK20260910-483920</p>
 * <pre>
 * 成功：HTTP 200 {"success":true,"applyNo":"...","status":"PENDING","statusText":"待审核", ...}
 * 未找到：HTTP 404 {"code":404,"message":"未找到该申请编号对应的记录"}
 * 参数错误：HTTP 400 {"code":400,"message":"...","errors":{"applyNo":"..."}}
 * </pre>
 */
@WebServlet(name = "statusServlet", urlPatterns = {"/api/applications/status"})
public class StatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Pattern APPLY_NO_PATTERN = Pattern.compile("LK\\d{8}-\\d{6}");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");

        String applyNo = req.getParameter("applyNo");
        String trimmed = applyNo == null ? "" : applyNo.trim();

        Map<String, String> errors = new LinkedHashMap<>();
        if (trimmed.isEmpty()) {
            errors.put("applyNo", "请输入申请编号");
        } else if (!APPLY_NO_PATTERN.matcher(trimmed).matches()) {
            errors.put("applyNo", "申请编号格式应为 LKxxxxxxxx-xxxxxx");
        }

        if (!errors.isEmpty()) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    errorBody(400, "查询参数有误", errors));
            return;
        }

        LabKeyApplication application = ApplicationRepository.getInstance().find(trimmed);
        if (application == null) {
            writeJson(resp, HttpServletResponse.SC_NOT_FOUND,
                    errorBody(404, "未找到该申请编号对应的记录，请核对后重试", null));
            return;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("applyNo", application.getApplyNo());
        data.put("name", application.getName());
        data.put("studentNo", application.getStudentNo());
        data.put("lab", application.getLab());
        data.put("useTime", application.getUseTime());
        data.put("createdAt", application.getCreatedAt());
        data.put("status", application.getStatus().name());
        data.put("statusText", application.getStatus().getText());
        writeJson(resp, HttpServletResponse.SC_OK, data);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            resp.setHeader("Allow", "GET");
            writeJson(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    errorBody(405, "仅支持 GET 方法", null));
            return;
        }
        super.service(req, resp);
    }
}
