package top.hcode.hoj.service.training;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.dto.ToJudgeDto;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.training.TrainingRecord;
import top.hcode.hoj.pojo.vo.TrainingRankVo;
import top.hcode.hoj.pojo.vo.UserRolesVo;

/**
 * @Author: Himit_ZH
 * @Date: 2021/11/21 23:38
 * @Description:
 */
public interface TrainingRecordService extends IService<TrainingRecord> {

    public CommonResult submitTrainingProblem(ToJudgeDto judgeDto, UserRolesVo userRolesVo, Judge judge);

    public IPage<TrainingRankVo> getTrainingRank(Long tid, int currentPage, int limit);
}