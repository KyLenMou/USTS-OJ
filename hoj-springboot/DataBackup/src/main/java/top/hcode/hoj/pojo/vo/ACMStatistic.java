package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: KyLen
 * @Date: 2024/2/23 22:25
 * @Description:
 */
@Data
@ApiModel(value="用户ACM比赛数据", description="")
public class ACMStatistic {
    @ApiModelProperty(value = "题目名称")
    private String title;

    @ApiModelProperty(value = "题目难度")
    private Integer difficulty;

    @ApiModelProperty(value = "提交次数 0未提交 >0提交通过 <0 提交未通过")
    private Integer total;

    @ApiModelProperty(value = "罚时")
    private Integer time;

    @ApiModelProperty(value = "是否是一血")
    private Integer first;
}
