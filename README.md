# 实验室钥匙申请系统

学生在线申请实验室钥匙的最小可运行示例：前端页面通过 **XHR 异步请求**提交申请，
Servlet 返回**申请编号**或**逐字段错误信息**；另提供状态查询接口。

- 后端：Java 8 + Servlet 4.0（Tomcat 9 / Jetty 9）+ Jackson
- 前端：原生 HTML / CSS / XMLHttpRequest（无框架）
- 存储：内存 `ConcurrentHashMap`（重启即清空，仅供演示）

## 目录结构

```
src/main/java/com/example/labkey/
├── model/
│   ├── ApplyStatus.java          # 状态枚举：待审核 / 已通过 / 已驳回
│   ├── ApplicationRequest.java   # 申请请求体 POJO
│   └── LabKeyApplication.java    # 申请记录实体
├── repo/
│   └── ApplicationRepository.java  # 内存仓库 + 申请编号生成 + 状态流转调度
├── servlet/
│   ├── ApplyServlet.java         # POST /api/applications 提交申请
│   └── StatusServlet.java        # GET  /api/applications/status 查询状态
└── util/
    └── JsonUtil.java             # JSON 与统一响应工具
src/main/webapp/
├── index.html                    # 申请 + 查询页面
├── css/style.css
├── js/app.js                     # XHR 封装：请求头 / 超时 / 错误回调
└── WEB-INF/web.xml               # UTF-8 编码、欢迎页
```

## 本地运行

### 方式一：Jetty 插件（推荐）

```bash
mvn jetty:run
```

打开 <http://localhost:8080/lab-key-apply/>

### 方式二：打 WAR 包丢进 Tomcat 9

```bash
mvn clean package
# 把 target/lab-key-apply.war 部署到 Tomcat 9 的 webapps/
```

## 接口说明

### 1. 提交申请

```
POST /lab-key-apply/api/applications
Content-Type: application/json; charset=UTF-8
Accept: application/json
X-Requested-With: XMLHttpRequest
```

请求体：

```json
{
  "name": "张三",
  "studentNo": "2026001234",
  "lab": "人工智能实验室",
  "useTime": "2026-09-12T14:30",
  "reason": "参加导师科研项目"
}
```

**成功（201 Created）**

```json
{
  "success": true,
  "applyNo": "LK20260910-483920",
  "name": "张三",
  "studentNo": "2026001234",
  "lab": "人工智能实验室",
  "useTime": "2026-09-12 14:30",
  "status": "PENDING",
  "statusText": "待审核",
  "message": "申请已提交，请妥善保管申请编号"
}
```

**字段校验失败（400 Bad Request）** —— `errors` 的 key 与表单字段名一一对应，
前端据此逐字段标红：

```json
{
  "code": 400,
  "message": "提交信息有误，请检查标红字段",
  "errors": {
    "name": "姓名长度需为 2~20 个字符",
    "studentNo": "学号需为 8~12 位数字",
    "lab": "所选实验室不在可申请范围内",
    "useTime": "使用时间不能早于当前时间"
  }
}
```

其他状态码：`415`（Content-Type 不是 JSON）、`405`（方法不对）。

校验规则：

| 字段 | 规则 |
| --- | --- |
| `name` | 必填，2~20 字符 |
| `studentNo` | 必填，8~12 位数字 |
| `lab` | 必填，须在白名单内（人工智能实验室 / 嵌入式系统实验室 / 网络安全实验室 / 数字媒体实验室 / 集成电路实验室） |
| `useTime` | 必填，ISO-8601（`yyyy-MM-ddTHH:mm`），不早于当前、不晚于 30 天后 |
| `reason` | 选填，不超过 200 字 |

### 2. 查询状态

```
GET /lab-key-apply/api/applications/status?applyNo=LK20260910-483920
Accept: application/json
```

成功（200）：

```json
{
  "success": true,
  "applyNo": "LK20260910-483920",
  "name": "张三",
  "studentNo": "2026001234",
  "lab": "人工智能实验室",
  "useTime": "2026-09-12 14:30",
  "createdAt": 1725955200000,
  "status": "PENDING",
  "statusText": "待审核"
}
```

- 参数格式错误：`400` + `errors.applyNo`
- 编号不存在：`404` + 中文提示

## 演示用状态流转

内存仓库内置了一个调度器，方便演示查询：

- 提交 **20 秒**后自动变为「已通过」；
- 若**学号末位为 4**，则 **15 秒**后变为「已驳回」。

前端查询区支持每 5 秒自动刷新，进入终态后自动停止。

## 前端异步细节（js/app.js）

- 使用原生 `XMLHttpRequest`，显式设置 `Content-Type: application/json; charset=UTF-8`、
  `Accept: application/json`、`X-Requested-With: XMLHttpRequest`；
- `xhr.timeout = 8000`，分别实现 `ontimeout` / `onerror` / `onabort` 回调，
  超时、断网、HTTP 错误、JSON 解析失败均有对应中文提示；
- 400 响应的 `errors` 映射到具体表单字段标红并聚焦；
- 提交期间按钮禁用并显示「提交中…」，完成后恢复。
