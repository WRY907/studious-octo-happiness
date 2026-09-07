package com.holocockpit.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.holocockpit.entity.*;
import com.holocockpit.mapper.*;
import cn.hutool.core.date.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Excel 数据导入服务（EasyExcel 简单模式 + Map 表头映射）
 * 支持8张表：phone/region/city/order/hot/traffic/profile/trend
 */
@Service
public class ImportService {

    @Resource
    private PhoneModelMapper phoneModelMapper;

    @Resource
    private RegionSalesMapper regionSalesMapper;

    @Resource
    private CitySalesMapper citySalesMapper;

    @Resource
    private RealtimeOrderMapper realtimeOrderMapper;

    @Resource
    private HotProductMapper hotProductMapper;

    @Resource
    private TrafficSourceMapper trafficSourceMapper;

    @Resource
    private UserProfileMapper userProfileMapper;

    @Resource
    private SalesTrendMapper salesTrendMapper;

    // ==================== 表元信息定义 ====================

    /**
     * 8张导入表的元信息（保持注册顺序）
     */
    private static final Map<String, TableMeta> TABLES = new LinkedHashMap<>();

    static {
        register("phone", "机型信息",
                new ColumnMeta[]{col("机型名称", "modelName", true), col("系列", "series", false),
                        col("价格", "price", true), col("评分", "rating", false)},
                Arrays.asList(
                        row("Mate 70 Pro", "Mate 系列", 6999, 4.9),
                        row("nova 13", "nova 系列", 2999, 4.7)));
        register("region", "省份销售",
                new ColumnMeta[]{col("省份", "province", true), col("销售额", "sales", true),
                        col("订单数", "orders", false), col("用户数", "users", false), col("日期", "statDate", true)},
                Arrays.asList(
                        row("广东", 15200000.50, 3500, 1200, "2026-09-03"),
                        row("北京", 9800000.00, 2200, 800, "2026-09-03")));
        register("city", "城市销售",
                new ColumnMeta[]{col("城市", "city", true), col("销售额", "sales", true),
                        col("订单数", "orders", false), col("用户数", "users", false), col("日期", "statDate", true)},
                Arrays.asList(
                        row("深圳", 5600000.00, 1300, 450, "2026-09-03"),
                        row("西安", 3200000.00, 800, 300, "2026-09-03")));
        register("order", "实时订单",
                new ColumnMeta[]{col("订单号", "orderNo", true), col("用户名", "userName", false),
                        col("机型名称", "modelName", true), col("金额", "amount", true),
                        col("省份", "province", false), col("城市", "city", false),
                        col("状态", "status", false), col("时间", "createTime", false)},
                Arrays.asList(
                        row("HW202609030001", "张三", "Mate 70 Pro", 6999, "广东", "深圳", 1, "2026-09-03 10:30:00"),
                        row("HW202609030002", "李四", "nova 13", 2999, "陕西", "西安", 2, "2026-09-03 11:20:00")));
        register("hot", "热销机型",
                new ColumnMeta[]{col("排名", "rankNo", false), col("机型名称", "modelName", true),
                        col("销量", "salesCount", false), col("销售额", "salesAmount", true),
                        col("增长率", "growth", false)},
                Arrays.asList(
                        row(1, "Mate 70 Pro", 3200, 22396800.00, 12.50),
                        row(2, "nova 13", 2800, 8397200.00, 8.30)));
        register("traffic", "流量来源",
                new ColumnMeta[]{col("一级分类", "category", true), col("二级来源", "sourceName", true),
                        col("访问量", "visits", false), col("占比", "ratio", false)},
                Arrays.asList(
                        row("线上渠道", "华为商城", 580000, 35.20),
                        row("搜索引擎", "百度搜索", 220000, 13.30)));
        register("profile", "用户画像",
                new ColumnMeta[]{col("类型", "profileType", true), col("名称", "profileName", true),
                        col("用户数", "userCount", false), col("占比", "ratio", false)},
                Arrays.asList(
                        row("gender", "男性", 580000, 58.00),
                        row("age", "18-24岁", 180000, 18.00)));
        register("trend", "销售趋势",
                new ColumnMeta[]{col("日期", "statDate", true), col("销售额", "sales", true),
                        col("订单数", "orders", false), col("访问量", "visits", false), col("客单价", "avgOrderValue", false)},
                Arrays.asList(
                        row("2026-09-03", 15200000.50, 3500, 580000, 4342.87),
                        row("2026-09-02", 14500000.00, 3400, 560000, 4264.71)));
    }

    /**
     * 模板示例行
     */
    private static List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    /**
     * 列元信息
     */
    private static class ColumnMeta {
        final String label;
        final String field;
        final boolean required;

        ColumnMeta(String label, String field, boolean required) {
            this.label = label;
            this.field = field;
            this.required = required;
        }
    }

    /**
     * 表元信息
     */
    private static class TableMeta {
        final String table;
        final String name;
        final ColumnMeta[] columns;
        final List<List<Object>> examples;

        TableMeta(String table, String name, ColumnMeta[] columns, List<List<Object>> examples) {
            this.table = table;
            this.name = name;
            this.columns = columns;
            this.examples = examples;
        }
    }

    private static ColumnMeta col(String label, String field, boolean required) {
        return new ColumnMeta(label, field, required);
    }

    private static void register(String table, String name, ColumnMeta[] columns, List<List<Object>> examples) {
        TABLES.put(table, new TableMeta(table, name, columns, examples));
    }

    // ==================== 对外接口 ====================

    /**
     * 返回8张表的元信息 [{table, name, columns:[{label, field, required}]}]
     */
    public List<Map<String, Object>> getMeta() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (TableMeta meta : TABLES.values()) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("table", meta.table);
            t.put("name", meta.name);
            List<Map<String, Object>> columns = new ArrayList<>();
            for (ColumnMeta c : meta.columns) {
                Map<String, Object> column = new LinkedHashMap<>();
                column.put("label", c.label);
                column.put("field", c.field);
                column.put("required", c.required);
                columns.add(column);
            }
            t.put("columns", columns);
            list.add(t);
        }
        return list;
    }

    /**
     * 内存生成导入模板 Excel（表头 + 2行示例数据）
     */
    public byte[] getTemplate(String table) {
        TableMeta meta = requireTable(table);
        List<List<String>> head = new ArrayList<>();
        for (ColumnMeta c : meta.columns) {
            head.add(Collections.singletonList(c.label));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).sheet(meta.name).head(head).doWrite(meta.examples);
        return out.toByteArray();
    }

    /**
     * 模板文件名，如 phone_template.xlsx
     */
    public String templateFilename(String table) {
        return requireTable(table).table + "_template.xlsx";
    }

    /**
     * 导入 Excel：按中文表头映射读取，逐行校验转换，错误行跳过并记录，全部失败不插入
     */
    public ImportResult importExcel(String table, MultipartFile file) {
        requireTable(table);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的Excel文件");
        }

        // 读取表头与数据行
        Map<Integer, String> header = new HashMap<>();
        List<Map<Integer, String>> rows = readExcel(file, header);

        // 中文表头 → 列下标
        Map<String, Integer> colIndex = new HashMap<>();
        for (Map.Entry<Integer, String> e : header.entrySet()) {
            if (e.getValue() != null && !e.getValue().trim().isEmpty()) {
                colIndex.put(e.getValue().trim(), e.getKey());
            }
        }
        TableMeta meta = TABLES.get(table);
        for (ColumnMeta c : meta.columns) {
            if (!colIndex.containsKey(c.label)) {
                throw new IllegalArgumentException("表头缺少列【" + c.label + "】，请下载最新模板后填写");
            }
        }

        ImportResult result = new ImportResult();
        // 第一条数据行对应的 Excel 行号（表头占第1行）
        int excelRow = 1;
        List<RowEntity> valid = new ArrayList<>();
        for (Map<Integer, String> row : rows) {
            excelRow++;
            if (isEmptyRow(row)) {
                continue;
            }
            result.setTotal(result.getTotal() + 1);
            try {
                Object entity = parseRow(table, new RowData(row, colIndex));
                valid.add(new RowEntity(excelRow, entity));
            } catch (RowException e) {
                result.getErrors().add(new ImportResult.RowError(excelRow, e.getMessage()));
            }
        }

        // 全部失败则不插入
        if (!valid.isEmpty()) {
            saveAll(table, valid, result);
        }
        result.setFail(result.getErrors().size());
        result.setSuccess(result.getTotal() - result.getFail());
        return result;
    }

    // ==================== 读取与解析 ====================

    /**
     * 无模型简单模式读取：捕获表头行 + 全部数据行（Map<列下标, 单元格字符串>）
     */
    private List<Map<Integer, String>> readExcel(MultipartFile file, Map<Integer, String> header) {
        List<Map<Integer, String>> rows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
                @Override
                public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                    header.putAll(headMap);
                }

                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    rows.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet(0).doRead();
        } catch (IOException e) {
            throw new IllegalArgumentException("Excel 文件读取失败，请确认为有效的 xlsx 文件");
        } catch (Exception e) {
            throw new IllegalArgumentException("Excel 文件解析失败，请下载最新模板后填写");
        }
        return rows;
    }

    private boolean isEmptyRow(Map<Integer, String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (String v : row.values()) {
            if (v != null && !v.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 按表逐行转换为实体（校验必填/数字/日期/枚举）
     */
    private Object parseRow(String table, RowData r) {
        switch (table) {
            case "phone": {
                PhoneModel e = new PhoneModel();
                e.setModelName(r.reqStr("机型名称"));
                e.setSeries(r.optStr("系列"));
                e.setPrice(r.reqDec("价格"));
                e.setRating(r.optDec("评分"));
                return e;
            }
            case "region": {
                RegionSales e = new RegionSales();
                e.setProvince(r.reqStr("省份"));
                e.setSales(r.reqDec("销售额"));
                e.setOrders(r.optInt("订单数"));
                e.setUsers(r.optInt("用户数"));
                e.setStatDate(r.reqDate("日期"));
                return e;
            }
            case "city": {
                CitySales e = new CitySales();
                e.setCity(r.reqStr("城市"));
                e.setSales(r.reqDec("销售额"));
                e.setOrders(r.optInt("订单数"));
                e.setUsers(r.optInt("用户数"));
                e.setStatDate(r.reqDate("日期"));
                return e;
            }
            case "order": {
                RealtimeOrder e = new RealtimeOrder();
                e.setOrderNo(r.reqStr("订单号"));
                e.setUserName(r.optStr("用户名"));
                e.setModelName(r.reqStr("机型名称"));
                e.setAmount(r.reqDec("金额"));
                e.setProvince(r.optStr("省份"));
                e.setCity(r.optStr("城市"));
                Integer status = r.optInt("状态");
                if (status != null && (status < 1 || status > 4)) {
                    throw new RowException("【状态】只能是1-4（1待付款/2已付款/3已发货/4已完成）");
                }
                e.setStatus(status != null ? status : 1);
                LocalDateTime time = r.optDateTime("时间");
                e.setCreateTime(time != null ? time : LocalDateTime.now());
                return e;
            }
            case "hot": {
                HotProduct e = new HotProduct();
                e.setRankNo(r.optInt("排名"));
                e.setModelName(r.reqStr("机型名称"));
                e.setSalesCount(r.optInt("销量"));
                e.setSalesAmount(r.reqDec("销售额"));
                e.setGrowth(r.optDec("增长率"));
                return e;
            }
            case "traffic": {
                TrafficSource e = new TrafficSource();
                e.setCategory(r.reqStr("一级分类"));
                e.setSourceName(r.reqStr("二级来源"));
                e.setVisits(r.optLong("访问量"));
                e.setRatio(r.optDec("占比"));
                return e;
            }
            case "profile": {
                UserProfile e = new UserProfile();
                String type = r.reqStr("类型").toLowerCase();
                if (!"gender".equals(type) && !"age".equals(type)) {
                    throw new RowException("【类型】只能是 gender 或 age");
                }
                e.setProfileType(type);
                e.setProfileName(r.reqStr("名称"));
                e.setUserCount(r.optInt("用户数"));
                e.setRatio(r.optDec("占比"));
                return e;
            }
            case "trend": {
                SalesTrend e = new SalesTrend();
                e.setStatDate(r.reqDate("日期"));
                e.setSales(r.reqDec("销售额"));
                e.setOrders(r.optInt("订单数"));
                e.setVisits(r.optInt("访问量"));
                e.setAvgOrderValue(r.optDec("客单价"));
                return e;
            }
            default:
                throw new IllegalArgumentException("不支持的数据表：" + table);
        }
    }

    // ==================== 落库 ====================

    /**
     * 逐条插入（phone 机型名重复则更新、trend 日期重复则更新）
     */
    @SuppressWarnings("unchecked")
    private void saveAll(String table, List<RowEntity> list, ImportResult result) {
        switch (table) {
            case "phone": {
                // 机型名重复则更新
                Map<String, Long> existing = new HashMap<>();
                for (PhoneModel p : phoneModelMapper.selectList(null)) {
                    existing.put(p.getModelName(), p.getId());
                }
                for (RowEntity re : list) {
                    PhoneModel p = (PhoneModel) re.entity;
                    try {
                        Long id = existing.get(p.getModelName());
                        if (id != null) {
                            p.setId(id);
                            phoneModelMapper.updateById(p);
                        } else {
                            phoneModelMapper.insert(p);
                            existing.put(p.getModelName(), p.getId());
                        }
                    } catch (Exception e) {
                        result.getErrors().add(new ImportResult.RowError(re.row, "保存失败：" + e.getMessage()));
                    }
                }
                break;
            }
            case "trend": {
                // 日期重复则更新
                Map<LocalDate, Long> existing = new HashMap<>();
                for (SalesTrend t : salesTrendMapper.selectList(null)) {
                    existing.put(t.getStatDate(), t.getId());
                }
                for (RowEntity re : list) {
                    SalesTrend t = (SalesTrend) re.entity;
                    try {
                        Long id = existing.get(t.getStatDate());
                        if (id != null) {
                            t.setId(id);
                            salesTrendMapper.updateById(t);
                        } else {
                            salesTrendMapper.insert(t);
                            existing.put(t.getStatDate(), t.getId());
                        }
                    } catch (Exception e) {
                        result.getErrors().add(new ImportResult.RowError(re.row, "保存失败：" + e.getMessage()));
                    }
                }
                break;
            }
            case "region": {
                for (RowEntity re : list) {
                    insertOne(re, result, regionSalesMapper::insert);
                }
                break;
            }
            case "city": {
                for (RowEntity re : list) {
                    insertOne(re, result, citySalesMapper::insert);
                }
                break;
            }
            case "order": {
                for (RowEntity re : list) {
                    insertOne(re, result, realtimeOrderMapper::insert);
                }
                break;
            }
            case "hot": {
                for (RowEntity re : list) {
                    insertOne(re, result, hotProductMapper::insert);
                }
                break;
            }
            case "traffic": {
                for (RowEntity re : list) {
                    insertOne(re, result, trafficSourceMapper::insert);
                }
                break;
            }
            case "profile": {
                for (RowEntity re : list) {
                    insertOne(re, result, userProfileMapper::insert);
                }
                break;
            }
            default:
                throw new IllegalArgumentException("不支持的数据表：" + table);
        }
    }

    /**
     * 单条插入，失败记录行号与原因
     */
    @SuppressWarnings("unchecked")
    private <T> void insertOne(RowEntity re, ImportResult result, java.util.function.Consumer<T> inserter) {
        try {
            inserter.accept((T) re.entity);
        } catch (Exception e) {
            result.getErrors().add(new ImportResult.RowError(re.row, "保存失败：" + e.getMessage()));
        }
    }

    // ==================== 辅助类型 ====================

    /**
     * 已转换的实体与其 Excel 行号
     */
    private static class RowEntity {
        final int row;
        final Object entity;

        RowEntity(int row, Object entity) {
            this.row = row;
            this.entity = entity;
        }
    }

    /**
     * 行数据读取/校验辅助：按中文表头取值并做类型转换
     */
    private static class RowData {
        private final Map<Integer, String> row;
        private final Map<String, Integer> colIndex;

        RowData(Map<Integer, String> row, Map<String, Integer> colIndex) {
            this.row = row;
            this.colIndex = colIndex;
        }

        String optStr(String label) {
            Integer idx = colIndex.get(label);
            if (idx == null) {
                return null;
            }
            String v = row.get(idx);
            if (v == null) {
                return null;
            }
            v = v.trim();
            return v.isEmpty() ? null : v;
        }

        String reqStr(String label) {
            String v = optStr(label);
            if (v == null) {
                throw new RowException("【" + label + "】不能为空");
            }
            return v;
        }

        BigDecimal reqDec(String label) {
            String v = reqStr(label);
            return dec(label, v);
        }

        BigDecimal optDec(String label) {
            String v = optStr(label);
            return v == null ? null : dec(label, v);
        }

        Integer optInt(String label) {
            String v = optStr(label);
            return v == null ? null : (int) (long) integer(label, v);
        }

        Long optLong(String label) {
            String v = optStr(label);
            return v == null ? null : integer(label, v);
        }

        LocalDate reqDate(String label) {
            String v = reqStr(label);
            return date(label, v);
        }

        LocalDate optDate(String label) {
            String v = optStr(label);
            return v == null ? null : date(label, v);
        }

        LocalDateTime optDateTime(String label) {
            String v = optStr(label);
            return v == null ? null : dateTime(label, v);
        }

        private BigDecimal dec(String label, String v) {
            String s = v.replace(",", "").replace("，", "").trim();
            if (s.endsWith("%")) {
                s = s.substring(0, s.length() - 1).trim();
            }
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                throw new RowException("【" + label + "】不是有效数字：" + v);
            }
        }

        private long integer(String label, String v) {
            try {
                return Long.parseLong(v.trim());
            } catch (NumberFormatException ignore) {
                // 兼容 "3500.0" 形式
                try {
                    return new BigDecimal(v.trim()).longValueExact();
                } catch (NumberFormatException | ArithmeticException e) {
                    throw new RowException("【" + label + "】不是有效整数：" + v);
                }
            }
        }

        private LocalDate date(String label, String v) {
            try {
                return LocalDate.parse(v.trim());
            } catch (Exception ignore) {
                // 继续尝试其他格式
            }
            try {
                return DateUtil.parse(v.trim()).toSqlDate().toLocalDate();
            } catch (Exception e) {
                throw new RowException("【" + label + "】日期格式应为 yyyy-MM-dd：" + v);
            }
        }

        private LocalDateTime dateTime(String label, String v) {
            try {
                return LocalDateTime.parse(v.trim());
            } catch (Exception ignore) {
                // 继续尝试其他格式
            }
            try {
                return DateUtil.parse(v.trim()).toLocalDateTime();
            } catch (Exception e) {
                throw new RowException("【" + label + "】时间格式应为 yyyy-MM-dd HH:mm:ss：" + v);
            }
        }
    }

    /**
     * 行级校验异常
     */
    private static class RowException extends RuntimeException {
        RowException(String message) {
            super(message);
        }
    }

    /**
     * 校验表代码，非法则抛出 IllegalArgumentException
     */
    private TableMeta requireTable(String table) {
        TableMeta meta = table == null ? null : TABLES.get(table);
        if (meta == null) {
            throw new IllegalArgumentException("不支持的数据表：" + table + "，可选：" + TABLES.keySet());
        }
        return meta;
    }

    /**
     * 是否支持该表（供控制器校验使用）
     */
    public boolean supports(String table) {
        return table != null && TABLES.containsKey(table);
    }
}
