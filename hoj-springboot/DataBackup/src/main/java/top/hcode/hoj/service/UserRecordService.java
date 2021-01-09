package top.hcode.hoj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.hcode.hoj.pojo.vo.ACMRankVo;
import top.hcode.hoj.pojo.entity.UserRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import top.hcode.hoj.pojo.vo.OIRankVo;
import top.hcode.hoj.pojo.vo.UserHomeVo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Himit_ZH
 * @since 2020-10-23
 */
public interface UserRecordService extends IService<UserRecord> {

    Page<ACMRankVo> getACMRankList(int limit, int currentPage);

    Page<OIRankVo> getOIRankList(int limit, int currentPage);

    UserHomeVo getUserHomeInfo(String uid);

}