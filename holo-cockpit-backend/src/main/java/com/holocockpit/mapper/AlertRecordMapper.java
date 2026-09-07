package com.holocockpit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.holocockpit.entity.AlertRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能预警记录 Mapper
 */
@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {
}
