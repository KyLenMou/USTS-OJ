package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * @Author: KyLen
 * @Date: 2024/2/23 8:59
 * @Description:
 */
@Data
@ApiModel(value="用户比赛数据", description="")
public class UserContestInfoVO {
    @ApiModelProperty(value = "排序")
    private Integer order;
    @ApiModelProperty(value = "比赛id")
    private Long contestId;
    @ApiModelProperty(value = "比赛名称")
    private String contestName;
    @ApiModelProperty(value = "0为ACM 1为OI")
    private Integer contestType;
    @ApiModelProperty(value = "OI数据")
    private OIStatisticVO OIStatistic;
    @ApiModelProperty(value = "ACM数据")
    private ACMStatisticVO ACMStatistic;
    @ApiModelProperty(value = "排名")
    private Integer rank;
    @ApiModelProperty(value = "击败百分比")
    private Double beatPercent;
    @ApiModelProperty(value = "提交统计<提交状态,提交次数>")
    private Map<String, Integer> submitStatistic;
}
