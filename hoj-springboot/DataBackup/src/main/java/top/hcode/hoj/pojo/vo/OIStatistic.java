package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: KyLen
 * @Date: 2024/2/23 22:26
 * @Description:
 */
@Data
@ApiModel(value="用户ACM比赛数据", description="")
public class OIStatistic {
    @ApiModelProperty(value = "题目名称")
    private String title;

    @ApiModelProperty(value = "题目难度")
    private Integer difficulty;

    @ApiModelProperty(value = "1满分通过0未提交-1未通过")
    private Integer status;

    @ApiModelProperty(value = "分数")
    private Integer score;

    @ApiModelProperty(value = "罚时")
    private Integer time;
}
