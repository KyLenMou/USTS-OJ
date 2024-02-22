package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: KyLen
 * @Date: 2024/2/5 22:40
 * @Description:
 */
@Data
@AllArgsConstructor
@ApiModel(value="模型数据", description="")
public class ModelAxisData {
    @ApiModelProperty(value = "x轴坐标")
    private Integer x;

    @ApiModelProperty(value = "y轴坐标")
    private Integer y;

    @ApiModelProperty(value = "达成度")
    private Double rate;
}
