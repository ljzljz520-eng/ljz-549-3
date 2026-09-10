/**
 * 实验室钥匙申请 - 前端逻辑
 * 基于原生 XMLHttpRequest，显式处理请求头、超时与错误回调。
 */
(function () {
    'use strict';

    // 接口地址（相对路径，跟随当前页面同源部署）
    var API_APPLY = 'api/applications';
    var API_STATUS = 'api/applications/status';

    // 请求超时时间（毫秒）
    var REQUEST_TIMEOUT = 8000;
    // 自动刷新间隔（毫秒）
    var AUTO_REFRESH_INTERVAL = 5000;

    /* ------------------------------------------------------------------ *
     * 通用 XHR 请求封装
     * options: {
     *   method, url, params(object), body(object), timeout,
     *   onSuccess(data, status, xhr),
     *   onError(err), onTimeout(xhr), onAbort(xhr), onComplete()
     * }
     * ------------------------------------------------------------------ */
    function request(options) {
        var method = (options.method || 'GET').toUpperCase();
        var url = options.url;
        var timeout = options.timeout != null ? options.timeout : REQUEST_TIMEOUT;

        if (options.params) {
            var query = Object.keys(options.params)
                .filter(function (k) { return options.params[k] !== '' && options.params[k] != null; })
                .map(function (k) { return encodeURIComponent(k) + '=' + encodeURIComponent(options.params[k]); })
                .join('&');
            if (query) {
                url += (url.indexOf('?') === -1 ? '?' : '&') + query;
            }
        }

        var xhr = new XMLHttpRequest();
        xhr.open(method, url, true);
        xhr.timeout = timeout;

        var payload = null;
        if (options.body !== undefined && options.body !== null) {
            payload = JSON.stringify(options.body);
            // 请求头写完整：JSON、UTF-8、X-Requested-With、Accept
            xhr.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');
        }
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');

        // 超时回调
        xhr.ontimeout = function () {
            if (options.onTimeout) {
                options.onTimeout(xhr);
            } else if (options.onError) {
                options.onError({ kind: 'timeout', message: '请求超时，请检查网络后重试' });
            }
            if (options.onComplete) options.onComplete();
        };

        // 网络层错误回调（断网 / 跨域被拒 / DNS 失败等）
        xhr.onerror = function () {
            if (options.onError) {
                options.onError({ kind: 'network', message: '网络异常，无法连接到服务器' });
            }
            if (options.onComplete) options.onComplete();
        };

        // 主动中止回调
        xhr.onabort = function () {
            if (options.onAbort) {
                options.onAbort(xhr);
            }
            if (options.onComplete) options.onComplete();
        };

        xhr.onreadystatechange = function () {
            if (xhr.readyState !== XMLHttpRequest.DONE) return;
            // ont­imeout / onerror / onabort 场景下 status 为 0，已在各自回调处理
            if (xhr.status === 0) return;

            var data = null;
            var parseError = false;
            try {
                data = xhr.responseText ? JSON.parse(xhr.responseText) : null;
            } catch (e) {
                parseError = true;
            }

            if (xhr.status >= 200 && xhr.status < 300 && !parseError) {
                if (options.onSuccess) options.onSuccess(data, xhr.status, xhr);
            } else {
                if (options.onError) {
                    if (parseError) {
                        options.onError({ kind: 'http', status: xhr.status, message: '服务器返回了无法解析的响应' });
                    } else {
                        options.onError({
                            kind: 'http',
                            status: xhr.status,
                            code: data && data.code,
                            message: (data && data.message) || ('请求失败（HTTP ' + xhr.status + '）'),
                            errors: data && data.errors
                        });
                    }
                }
            }
            if (options.onComplete) options.onComplete();
        };

        xhr.send(payload);
        return xhr;
    }

    /* ------------------------------------------------------------------ *
     * DOM 工具
     * ------------------------------------------------------------------ */
    function $(id) { return document.getElementById(id); }

    function escapeHtml(value) {
        if (value == null) return '';
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function showBanner(el, message) {
        el.textContent = message;
        el.hidden = false;
    }

    function hideBanner(el) {
        el.hidden = true;
        el.textContent = '';
    }

    function clearFieldErrors(scopeEl) {
        var nodes = scopeEl.querySelectorAll('.field-error');
        Array.prototype.forEach.call(nodes, function (p) {
            p.hidden = true;
            p.textContent = '';
        });
        var inputs = scopeEl.querySelectorAll('.invalid');
        Array.prototype.forEach.call(inputs, function (i) { i.classList.remove('invalid'); });
    }

    function showFieldErrors(scopeEl, errors) {
        Object.keys(errors || {}).forEach(function (field) {
            var p = scopeEl.querySelector('[data-error-for="' + field + '"]');
            if (p) {
                p.textContent = errors[field];
                p.hidden = false;
                var input = scopeEl.querySelector('[name="' + field + '"]');
                if (input) input.classList.add('invalid');
            }
        });
        // 聚焦到第一个出错的字段
        var firstKey = Object.keys(errors || {})[0];
        if (firstKey) {
            var first = scopeEl.querySelector('[name="' + firstKey + '"]');
            if (first) first.focus();
        }
    }

    function setLoading(btn, loading) {
        var text = btn.querySelector('.btn-text');
        var loadingText = btn.querySelector('.btn-loading');
        btn.disabled = loading;
        btn.classList.toggle('is-loading', loading);
        if (text) text.hidden = loading;
        if (loadingText) loadingText.hidden = !loading;
    }

    function setMinUseTime() {
        var now = new Date();
        now.setSeconds(0, 0);
        var pad = function (n) { return String(n).padStart(2, '0'); };
        var min = now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate())
            + 'T' + pad(now.getHours()) + ':' + pad(now.getMinutes());
        $('useTime').min = min;
        var maxDate = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);
        $('useTime').max = maxDate.getFullYear() + '-' + pad(maxDate.getMonth() + 1)
            + '-' + pad(maxDate.getDate()) + 'T23:59';
    }

    function formatTimestamp(ts) {
        var d = new Date(ts);
        var pad = function (n) { return String(n).padStart(2, '0'); };
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
            + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
    }

    /* ------------------------------------------------------------------ *
     * 1. 提交申请
     * ------------------------------------------------------------------ */
    function initApplyForm() {
        var form = $('apply-form');
        var banner = $('apply-banner');
        var btn = $('apply-submit');

        form.addEventListener('submit', function (e) {
            e.preventDefault();
            clearFieldErrors(form);
            hideBanner(banner);

            var payload = {
                name: $('name').value.trim(),
                studentNo: $('studentNo').value.trim(),
                lab: $('lab').value,
                useTime: $('useTime').value,
                reason: $('reason').value.trim()
            };

            setLoading(btn, true);

            request({
                method: 'POST',
                url: API_APPLY,
                body: payload,
                timeout: REQUEST_TIMEOUT,

                onSuccess: function (data) {
                    form.hidden = true;
                    $('apply-success').hidden = false;
                    $('result-apply-no').textContent = data.applyNo;
                    $('result-status').textContent = data.statusText || '待审核';
                    // 成功后自动带入查询框，便于直接查询
                    $('query-apply-no').value = data.applyNo;
                },

                onError: function (err) {
                    if (err.kind === 'http' && err.status === 400 && err.errors) {
                        // 字段级错误：逐字段标红
                        showFieldErrors(form, err.errors);
                        showBanner(banner, err.message || '提交信息有误，请检查标红字段');
                    } else if (err.kind === 'timeout') {
                        showBanner(banner, '请求超时（' + REQUEST_TIMEOUT / 1000 + ' 秒），请稍后重试或检查网络');
                    } else if (err.kind === 'network') {
                        showBanner(banner, '网络连接失败，请确认服务器已启动后重试');
                    } else {
                        showBanner(banner, err.message || ('提交失败（HTTP ' + err.status + '）'));
                    }
                },

                onTimeout: function () {
                    showBanner(banner, '服务器响应超时，请稍后重试');
                },

                onAbort: function () {
                    showBanner(banner, '请求已被取消');
                },

                onComplete: function () {
                    setLoading(btn, false);
                }
            });
        });

        $('goto-query').addEventListener('click', function () {
            $('query-card').scrollIntoView({ behavior: 'smooth', block: 'start' });
            $('query-submit').click();
        });

        $('apply-again').addEventListener('click', function () {
            form.reset();
            clearFieldErrors(form);
            hideBanner(banner);
            $('apply-success').hidden = true;
            form.hidden = false;
            $('name').focus();
        });
    }

    /* ------------------------------------------------------------------ *
     * 2. 状态查询
     * ------------------------------------------------------------------ */
    var refreshTimer = null;
    var lastQueryNo = null;

    function renderStatus(data) {
        var panel = $('status-result');
        panel.hidden = false;

        var badge = $('query-status-badge');
        badge.textContent = data.statusText || data.status;
        badge.className = 'badge ' +
            (data.status === 'APPROVED' ? 'badge-approved'
                : data.status === 'REJECTED' ? 'badge-rejected' : 'badge-pending');

        $('d-apply-no').textContent = data.applyNo;
        $('d-name').textContent = data.name;
        $('d-student-no').textContent = data.studentNo;
        $('d-lab').textContent = data.lab;
        $('d-use-time').textContent = data.useTime;
        $('d-created-at').textContent = formatTimestamp(data.createdAt);
    }

    function doQuery(silent) {
        var form = $('query-form');
        var banner = $('query-banner');
        var btn = $('query-submit');
        var applyNo = $('query-apply-no').value.trim();

        clearFieldErrors(form);
        if (!silent) hideBanner(banner);

        setLoading(btn, true);

        request({
            method: 'GET',
            url: API_STATUS,
            params: { applyNo: applyNo },
            timeout: REQUEST_TIMEOUT,

            onSuccess: function (data) {
                lastQueryNo = applyNo;
                hideBanner(banner);
                renderStatus(data);

                // 进入终态后自动停止刷新
                if (data.status === 'APPROVED' || data.status === 'REJECTED') {
                    stopAutoRefresh();
                }
            },

            onError: function (err) {
                $('status-result').hidden = true;
                if (err.kind === 'http' && err.status === 400 && err.errors) {
                    showFieldErrors(form, err.errors);
                    if (!silent) showBanner(banner, err.message || '查询参数有误');
                } else if (err.kind === 'http' && err.status === 404) {
                    if (!silent) showBanner(banner, err.message || '未找到该申请记录');
                } else if (err.kind === 'timeout') {
                    if (!silent) showBanner(banner, '查询超时，请稍后重试');
                } else if (err.kind === 'network') {
                    if (!silent) showBanner(banner, '网络连接失败，请确认服务器已启动');
                } else {
                    if (!silent) showBanner(banner, err.message || ('查询失败（HTTP ' + err.status + '）'));
                }
            },

            onTimeout: function () {
                if (!silent) showBanner(banner, '服务器响应超时，请稍后重试');
            },

            onAbort: function () {
                if (!silent) showBanner(banner, '查询已被取消');
            },

            onComplete: function () {
                setLoading(btn, false);
            }
        });
    }

    function stopAutoRefresh() {
        var cb = $('auto-refresh');
        if (refreshTimer) {
            clearInterval(refreshTimer);
            refreshTimer = null;
        }
        cb.checked = false;
    }

    function initQueryForm() {
        $('query-form').addEventListener('submit', function (e) {
            e.preventDefault();
            doQuery(false);
        });

        $('query-apply-no').addEventListener('input', function () {
            stopAutoRefresh();
        });

        $('auto-refresh').addEventListener('change', function () {
            if (this.checked) {
                if (!lastQueryNo || $('query-apply-no').value.trim() !== lastQueryNo) {
                    doQuery(false);
                }
                refreshTimer = setInterval(function () {
                    if (lastQueryNo) doQuery(true);
                }, AUTO_REFRESH_INTERVAL);
            } else if (refreshTimer) {
                clearInterval(refreshTimer);
                refreshTimer = null;
            }
        });
    }

    /* ------------------------------------------------------------------ *
     * 启动
     * ------------------------------------------------------------------ */
    document.addEventListener('DOMContentLoaded', function () {
        setMinUseTime();
        initApplyForm();
        initQueryForm();
    });
})();
