package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import top.hcode.hoj.pojo.entity.problem.Tag;

import java.util.List;
import java.util.Map;

/**
 * @Author: KyLen
 * @Date: 2024/1/31 22:55
 * @Description:
 */
@Data
@ApiModel(value="标签难度统计", description="")
public class TagDifficultyStatisticVO {
    @ApiModelProperty(value = "标签统计")
    private Map<String,Long> tagStatistics;
    @ApiModelProperty(value = "难度统计")
    private Map<Integer,Long> difficultyStatistics;
}
