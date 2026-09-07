package com.holocockpit.controller;

import cn.hutool.json.JSONUtil;
import com.holocockpit.common.Result;
import com.holocockpit.service.ImportResult;
import com.holocockpit.service.ImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Excel 数据导入接口（JWT 保护，路径 /admin/import/*，仅ADMIN）
 */
@RestController
@RequestMapping("/admin/import")
public class ImportController {

    /**
     * xlsx 文件 MIME 类型
     */
    private static final String XLSX_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Resource
    private ImportService importService;

    /**
     * 导入 Excel：POST /admin/import/{table}（multipart 文件字段名 file）
     */
    @PostMapping("/{table}")
    public Result<ImportResult> importExcel(@PathVariable String table,
                                            @RequestParam("file") MultipartFile file,
                                            HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可导入数据");
        }
        if (!importService.supports(table)) {
            return Result.error(400, "不支持的数据表：" + table);
        }
        try {
            return Result.success("导入完成", importService.importExcel(table, file));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "导入失败：" + e.getMessage());
        }
    }

    /**
     * 下载导入模板：GET /admin/import/{table}/template
     */
    @GetMapping("/{table}/template")
    public ResponseEntity<?> template(@PathVariable String table, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).contentType(MediaType.APPLICATION_JSON)
                    .body(JSONUtil.toJsonStr(Result.error(403, "无权限，仅管理员可下载模板")));
        }
        if (!importService.supports(table)) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(JSONUtil.toJsonStr(Result.error(400, "不支持的数据表：" + table)));
        }
        byte[] data = importService.getTemplate(table);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(XLSX_MEDIA_TYPE))
                .header("Content-Disposition", "attachment; filename=" + table + "_template.xlsx")
                .body(data);
    }

    /**
     * 8张表的导入元信息：GET /admin/import/meta
     */
    @GetMapping("/meta")
    public Result<List<Map<String, Object>>> meta(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可查看");
        }
        return Result.success(importService.getMeta());
    }

    private boolean isAdmin(HttpServletRequest request) {
        return "ADMIN".equals(request.getAttribute("role"));
    }
}
