package com.holocockpit.service;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入结果
 */
@Data
public class ImportResult {

    /**
     * 总行数（有效数据行）
     */
    private int total;

    /**
     * 成功行数
     */
    private int success;

    /**
     * 失败行数
     */
    private int fail;

    /**
     * 错误明细
     */
    private List<RowError> errors = new ArrayList<>();

    @Data
    public static class RowError {

        /**
         * Excel 行号（含表头行，从1开始）
         */
        private int row;

        /**
         * 错误原因
         */
        private String message;

        public RowError() {
        }

        public RowError(int row, String message) {
            this.row = row;
            this.message = message;
        }
    }
}
