package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @Author: KyLen
 * @Date: 2024/2/4 23:27
 * @Description:
 */
@Data
@ApiModel(value="模型分析", description="")
public class ModelDataVO {
    // 前端写死
    // @ApiModelProperty(value = "x轴数据(等级,通过率,解题量)")
    // private List<String> xaxisData;

    @ApiModelProperty(value = "y轴数据(标签)")
    private List<String> yaxisData;

    @ApiModelProperty(value = "坐标数据")
    private List<ModelAxisData> data;
}
