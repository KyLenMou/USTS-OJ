package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @Author: KyLen
 * @Date: 2024/2/22 20:06
 * @Description:
 */
@ApiModel(value = "个性化推荐题目", description = "")
@Data
public class RecommendProblemVO {
    @ApiModelProperty(value = "pid")
    private String pid;
    @ApiModelProperty(value = "题目名称")
    private String title;
    @ApiModelProperty(value = "题目难度")
    private Integer difficulty;

}
