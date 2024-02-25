package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @Author: KyLen
 * @Date: 2024/2/23 9:15
 * @Description:
 */
@Data
@ApiModel(value = "用户OI比赛总数据", description = "")
public class OIStatisticVO {
    @ApiModelProperty(value = "比赛题目ids")
    private List<String> pids;
    @ApiModelProperty(value = "题目")
    private List<List<OIStatistic>> statistics;
}
