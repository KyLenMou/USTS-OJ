package top.hcode.hoj.manager.oj;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.exception.StatusSystemErrorException;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.contest.ContestRecordEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.dao.problem.TagClassificationEntityService;
import top.hcode.hoj.dao.problem.TagEntityService;
import top.hcode.hoj.dao.user.*;
import top.hcode.hoj.manager.email.EmailManager;
import top.hcode.hoj.mapper.JudgeMapper;
import top.hcode.hoj.mapper.ProblemTagMapper;
import top.hcode.hoj.pojo.dto.ChangeEmailDTO;
import top.hcode.hoj.pojo.dto.ChangePasswordDTO;
import top.hcode.hoj.pojo.dto.CheckUsernameOrEmailDTO;
import top.hcode.hoj.pojo.dto.ContestRankDTO;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.contest.ContestProblem;
import top.hcode.hoj.pojo.entity.contest.ContestRecord;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemTag;
import top.hcode.hoj.pojo.entity.problem.Tag;
import top.hcode.hoj.pojo.entity.problem.TagClassification;
import top.hcode.hoj.pojo.entity.user.Role;
import top.hcode.hoj.pojo.entity.user.Session;
import top.hcode.hoj.pojo.entity.user.UserAcproblem;
import top.hcode.hoj.pojo.entity.user.UserInfo;
import top.hcode.hoj.pojo.vo.*;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.RedisUtils;
import top.hcode.hoj.validator.CommonValidator;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/10 16:53
 * @Description:
 */
@Component
public class AccountManager {
    @Autowired
    private ProblemTagMapper problemTagMapper;
    @Autowired
    private TagEntityService tagEntityService;
    @Autowired
    private TagClassificationEntityService tagClassificationEntityService;
    @Autowired
    private JudgeMapper judgeMapper;
    @Autowired
    private ContestRecordEntityService contestRecordEntityService;
    @Autowired
    private ContestEntityService contestEntityService;
    @Autowired
    private ContestManager contestManager;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private UserInfoEntityService userInfoEntityService;

    @Autowired
    private UserRoleEntityService userRoleEntityService;

    @Autowired
    private UserRecordEntityService userRecordEntityService;

    @Autowired
    private UserAcproblemEntityService userAcproblemEntityService;

    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private SessionEntityService sessionEntityService;

    @Autowired
    private CommonValidator commonValidator;

    @Autowired
    private EmailManager emailManager;

    /**
     * @MethodName checkUsernameOrEmail
     * @Params * @param null
     * @Description 检验用户名和邮箱是否存在
     * @Return
     * @Since 2020/11/5
     */
    public CheckUsernameOrEmailVO checkUsernameOrEmail(CheckUsernameOrEmailDTO checkUsernameOrEmailDto) {

        String email = checkUsernameOrEmailDto.getEmail();

        String username = checkUsernameOrEmailDto.getUsername();

        boolean rightEmail = false;

        boolean rightUsername = false;

        if (!StringUtils.isEmpty(email)) {
            email = email.trim();
            boolean isEmail = Validator.isEmail(email);
            if (!isEmail) {
                rightEmail = false;
            } else {
                QueryWrapper<UserInfo> wrapper = new QueryWrapper<UserInfo>().eq("email", email);
                UserInfo user = userInfoEntityService.getOne(wrapper, false);
                if (user != null) {
                    rightEmail = true;
                } else {
                    rightEmail = false;
                }
            }
        }

        if (!StringUtils.isEmpty(username)) {
            username = username.trim();
            QueryWrapper<UserInfo> wrapper = new QueryWrapper<UserInfo>().eq("username", username);
            UserInfo user = userInfoEntityService.getOne(wrapper, false);
            if (user != null) {
                rightUsername = true;
            } else {
                rightUsername = false;
            }
        }

        CheckUsernameOrEmailVO checkUsernameOrEmailVo = new CheckUsernameOrEmailVO();
        checkUsernameOrEmailVo.setEmail(rightEmail);
        checkUsernameOrEmailVo.setUsername(rightUsername);
        return checkUsernameOrEmailVo;
    }

    /**
     * @param uid
     * @param username
     * @MethodName getUserHomeInfo
     * @Description 前端userHome用户个人主页的数据请求，主要是返回解决题目数，AC的题目列表，提交总数，AC总数，Rating分，
     *              新增: 尚未涉足的标签,未通过的题目,标签难度统计,能力达成度模型数据
     * @Since 2021/01/07
     */
    public UserHomeVO getUserHomeInfo(String uid, String username) throws StatusFailException {

        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        boolean isMine = false;
        // 如果没有uid和username，默认查询当前登录用户的
        if (StringUtils.isEmpty(uid) && StringUtils.isEmpty(username)) {
            isMine = true;
            if (userRolesVo != null) {
                uid = userRolesVo.getUid();
            } else {
                throw new StatusFailException("请求参数错误：uid和username不能都为空！");
            }
        }

        UserHomeVO userHomeInfo = userRecordEntityService.getUserHomeInfo(uid, username);
        if (userHomeInfo == null) {
            throw new StatusFailException("用户不存在");
        }
        QueryWrapper<UserAcproblem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", userHomeInfo.getUid())
                .select("distinct pid", "submit_id")
                .orderByAsc("submit_id");

        List<UserAcproblem> acProblemList = userAcproblemEntityService.list(queryWrapper);
        // 还要加上contest的题目
        QueryWrapper<ContestRecord> contestRecordQueryWrapper = new QueryWrapper<>();
        contestRecordQueryWrapper.eq("uid", userHomeInfo.getUid())
                .select("distinct pid", "submit_id")
                .orderByAsc("submit_id");
        List<ContestRecord> contestRecordList = contestRecordEntityService.list(contestRecordQueryWrapper);
        for (ContestRecord contestRecord : contestRecordList) {
            UserAcproblem userAcproblem = new UserAcproblem();
            userAcproblem.setPid(contestRecord.getPid());
            userAcproblem.setSubmitId(contestRecord.getSubmitId());
            acProblemList.add(userAcproblem);
        }
        // 获得去重的pidList
        List<Long> pidList = acProblemList.stream().map(UserAcproblem::getPid).distinct().collect(Collectors.toList());

        // 将pidList加入到Set中,简化后续查询复杂度
        Set<Long> pidSet = new HashSet<>(pidList);

        // 标签难度统计
        TagDifficultyStatisticVO tagDifficultyStatisticVO = new TagDifficultyStatisticVO();

        // ! 如果用户没有过题, 返回为null, 但是前端需要一个数组而不是null, 否则前端会报错
        userHomeInfo.setSolvedList(new LinkedList<>());
        if (!CollUtil.isEmpty(pidList)) {
            QueryWrapper<Problem> problemQueryWrapper = new QueryWrapper<>();
            problemQueryWrapper.select("id", "problem_id", "difficulty");
            problemQueryWrapper.in("id", pidList);
            List<Problem> acProblems = problemEntityService.list(problemQueryWrapper);
            // 前端不需要了
            // Map<Integer, List<UserHomeProblemVO>> map = acProblems.stream()
            //         .map(this::convertProblemVO)
            //         .collect(Collectors.groupingBy(UserHomeProblemVO::getDifficulty));
            // userHomeInfo.setSolvedGroupByDifficulty(map);

            // 难度统计
            Map<Integer,Long> difficultyStatistics = acProblems.stream().collect(Collectors.groupingBy(Problem::getDifficulty, Collectors.counting()));
            tagDifficultyStatisticVO.setDifficultyStatistics(difficultyStatistics);

            List<String> disPlayIdList = acProblems.stream().map(Problem::getProblemId).collect(Collectors.toList());
            if (disPlayIdList != null) userHomeInfo.setSolvedList(disPlayIdList);
        }
        QueryWrapper<Session> sessionQueryWrapper = new QueryWrapper<>();
        sessionQueryWrapper.eq("uid", userHomeInfo.getUid())
                .orderByDesc("gmt_create")
                .last("limit 1");

        Session recentSession = sessionEntityService.getOne(sessionQueryWrapper, false);
        if (recentSession != null) {
            userHomeInfo.setRecentLoginTime(recentSession.getGmtCreate());
        }

        // 查对应的所有提交记录
        QueryWrapper<Judge> judgeQueryWrapper = new QueryWrapper<>();
        judgeQueryWrapper.eq("uid",userHomeInfo.getUid()).select("pid","display_pid","status");
        List<Judge> judgeList = judgeMapper.selectList(judgeQueryWrapper);

        // 标签分类
        QueryWrapper<TagClassification> tagClassificationQueryWrapper = new QueryWrapper<>();
        tagClassificationQueryWrapper.eq("oj","ME").orderByAsc("`rank`");
        List<TagClassification> classificationList = tagClassificationEntityService.list(tagClassificationQueryWrapper);

        // 从classificationList的到rank为0的classification
        TagClassification tagClassification = classificationList.stream().filter(c -> c.getRank() == 0).findFirst().orElse(null);
        Long mainClassificationId = tagClassification == null ? null : tagClassification.getId();

        // 所有tag(只包括标签rank为0的,不是id,是rank)
        QueryWrapper<Tag> tagQueryWrapper = new QueryWrapper<>();
        tagQueryWrapper.eq("oj","ME").isNull("gid").orderByAsc("id");
        List<Tag> allTagList = tagEntityService.list(tagQueryWrapper);
        List<Tag> tagList = new ArrayList<>();
        if (mainClassificationId != null) tagList = allTagList.stream().filter(t -> (t.getTcid() != null && t.getTcid() == mainClassificationId)).collect(Collectors.toList());
        List<Long> tagIdList = tagList.stream().map(Tag::getId).collect(Collectors.toList());
        // tag-id -> tag-name
        Map<Long,String> tidNameMap = tagList.stream().collect(Collectors.toMap(Tag::getId,Tag::getName));
        // 所有p -> tag
        List<ProblemTag> tagProblem = new LinkedList<>();
        QueryWrapper<ProblemTag> problemTagsQueryWrapper = null;
        if (!CollectionUtils.isEmpty(tagIdList)) {
            problemTagsQueryWrapper = new QueryWrapper<>();
            problemTagsQueryWrapper.in("tid",tagIdList).orderByAsc("tid");
        }
        tagProblem = problemTagMapper.selectList(problemTagsQueryWrapper);
        // p-id -> List<tag-id>
        Map<Long,List<Long>> pidTidMap = tagProblem.stream().collect(Collectors.groupingBy(ProblemTag::getPid,Collectors.mapping(ProblemTag::getTid,Collectors.toList())));
        // p-id -> List<tag-name>
        Map<Long,List<String>> pidTagNameMap = new HashMap<>();
        for (Long pid : pidTidMap.keySet()) {
            List<Long> tidList = pidTidMap.get(pid);
            List<String> tagNameList = new ArrayList<>();
            for (Long tid : tidList) {
                tagNameList.add(tidNameMap.get(tid));
            }
            pidTagNameMap.put(pid,tagNameList);
        }

        // 所有p(id和difficulty)
        QueryWrapper<Problem> problemQueryWrapper = new QueryWrapper<>();
        problemQueryWrapper.select("id","difficulty");
        List<Problem> problemList = problemEntityService.list(problemQueryWrapper);
        // p-id -> p-difficulty
        Map<Long,Integer> pidDifficultyMap = problemList.stream().collect(Collectors.toMap(Problem::getId,Problem::getDifficulty));

        // 初始化标签难度模型数据结构
        Map<String,Map<Integer,int[]>> tagDifficultyModelMap = new HashMap<>();
        for (Tag t : tagList) {
            Map<Integer,int[]> map = new HashMap<>();
            // 三个难度初始化
            map.put(0,new int[]{0,0});
            map.put(1,new int[]{0,0});
            map.put(2,new int[]{0,0});
            tagDifficultyModelMap.put(t.getName(),map);
        }
        // 得到所有题目
        QueryWrapper<Problem> allProblemQueryWrapper = new QueryWrapper<>();
        allProblemQueryWrapper.select("id","difficulty","title","problem_id").orderByAsc("difficulty");
        List<Problem> allProblemList = problemEntityService.list(allProblemQueryWrapper);
        // 把题目加入到tagDifficultyModelMap(统计总题数)
        for (Problem p : allProblemList) {
            if (pidTagNameMap.get(p.getId()) == null) continue;
            for (String tagName : pidTagNameMap.get(p.getId())) {
                Map<Integer,int[]> map = tagDifficultyModelMap.get(tagName);
                if (map == null) continue;
                int[] num = map.get(p.getDifficulty());
                num[1]++;
            }
        }
        // 统计过题数
        for (Long pid : pidList) {
            if (pidTagNameMap.get(pid) == null) continue;
            for (String tagName : pidTagNameMap.get(pid)) {
                Map<Integer,int[]> map = tagDifficultyModelMap.get(tagName);
                if (map == null) continue;
                int[] num = map.get(pidDifficultyMap.get(pid));
                num[0]++;
            }
        }
        // 标签对应的通过率
        Map<String, int[]> tagACRateModelMap = new HashMap<>();
        for (Tag t : tagList) {
            tagACRateModelMap.put(t.getName(),new int[]{0,0});
        }

        Map<String,Integer> unsolvedMap = new HashMap<>();
        for (Judge j : judgeList) {
            // 统计未通过题目的逻辑
            String displayPid = j.getDisplayPid();
            Integer status = j.getStatus();
            if (unsolvedMap.get(displayPid) == null) {
                unsolvedMap.put(displayPid,status);
            } else {
                if (unsolvedMap.get(displayPid) != 0) {
                    unsolvedMap.put(displayPid, status);
                }
            }
            // 统计通过率(已过题数/(已过题的总失败提交数+已过题数))
            Long pid = j.getPid();
            List<String> tags = pidTagNameMap.get(pid);
            if (pidTagNameMap.get(pid) == null) continue;
            for (String tagName : tags) {
                int[] acRate = tagACRateModelMap.get(tagName);
                if (acRate == null) continue;
                if (status == 0) {
                    acRate[1]++;
                }
                else {
                    acRate[0]++;
                }
                tagACRateModelMap.put(tagName,acRate);
            }

        }

        // 得到模型数据
        userHomeInfo.setModelData(getModelData(tagList,tagDifficultyModelMap,tagACRateModelMap));

        // 个性化推荐题目:每个标签推荐一个未做过的题目

        // tag-id -> List<p-id>
        Map<Long,List<Long>> tidPidTempMap = tagProblem.stream().collect(Collectors.groupingBy(ProblemTag::getTid,Collectors.mapping(ProblemTag::getPid,Collectors.toList())));
        // 去掉通过的题目
        Map<Long,List<Long>> tidPidMap = new HashMap<>();
        for (Long tid : tidPidTempMap.keySet()) {
            List<Long> pids = tidPidTempMap.get(tid);
            List<Long> temp = new ArrayList<>();
            for (Long pid : pids) {
                if (!pidSet.contains(pid)) {
                    temp.add(pid);
                }
            }
            tidPidMap.put(tid,temp);
        }
        List<Problem> problems = new ArrayList<>();
        // 把上面的allProblemList转换成map<pid,problem>
        Map<Long,Problem> pidProblemListMap = allProblemList.stream().collect(Collectors.toMap(Problem::getId, p -> p));
        for (Long tid : tidPidMap.keySet()) {
            for (Long pid : tidPidMap.get(tid)) {
                if (pidProblemListMap.get(pid) == null) continue;
                problems.add(pidProblemListMap.get(pid));
                break;
            }
        }
        // 把problems封装成List<RecommendProblemVO>
        List<RecommendProblemVO> recommendProblems = problems.stream().map(p -> {
            RecommendProblemVO recommendProblemVO = new RecommendProblemVO();
            recommendProblemVO.setDifficulty(p.getDifficulty());
            recommendProblemVO.setTitle(p.getTitle());
            recommendProblemVO.setPid(p.getProblemId());
            return recommendProblemVO;
        }).collect(Collectors.toList());
        // recommendProblems去重
        recommendProblems = recommendProblems.stream().distinct().collect(Collectors.toList());

        // 如果不是看自己的就不给推荐的题目
        if (isMine) {
            userHomeInfo.setRecommendProblems(recommendProblems);
        }

        QueryWrapper<Contest> contestQueryWrapper = new QueryWrapper<>();
        contestQueryWrapper.orderByDesc("id");
        List<Contest> allContestList = contestEntityService.list(contestQueryWrapper);

        List<UserContestInfoVO> contestData = new ArrayList<>();
        final int[] index = {0};
        allContestList.forEach(c -> {
            if (c.getStatus() != 1 || !c.getVisible()) return;
            ContestRankDTO contestRankDTO = new ContestRankDTO();
            contestRankDTO.setCid(c.getId());
            contestRankDTO.setLimit(1000000000);
            IPage contestRank = null;
            try {
                contestRank = contestManager.getContestRank(contestRankDTO);
            } catch (StatusFailException | StatusForbiddenException e) {
                return;
            }
            List records = contestRank.getRecords();
            if (records == null || records.isEmpty()) return;
            UserContestInfoVO userContestInfoVO = new UserContestInfoVO();
            if (records.get(0) instanceof OIContestRankVO) {
                List<OIContestRankVO> oiContestRankVOList = records;
                for (OIContestRankVO oiContestRankVO : oiContestRankVOList) {
                    // 查询当前用户是否在排名中
                    if (oiContestRankVO.getUid().equals(userHomeInfo.getUid())) {
                        // 查比赛题目
                        List<ContestProblemVO> contestProblem = null;
                        try {
                            contestProblem = contestManager.getContestProblem(c.getId(), true);
                        } catch (StatusFailException | StatusForbiddenException e) {
                            throw new RuntimeException(e);
                        }
                        // pid
                        List<String> pids = contestProblem.stream().map(ContestProblemVO::getDisplayId).collect(Collectors.toList());
                        // 对应的oiStatistics
                        List<OIStatistic> oiStatistics = contestProblem.stream().map(cp -> {
                            OIStatistic oiStatistic = new OIStatistic();
                            oiStatistic.setTitle(cp.getDisplayTitle());
                            oiStatistic.setDifficulty(pidDifficultyMap.get(cp.getPid()));
                            return oiStatistic;
                        }).collect(Collectors.toList());

                        // 根据比赛信息构建数据
                        userContestInfoVO.setOrder(index[0]++);
                        userContestInfoVO.setContestId(c.getId());
                        userContestInfoVO.setContestName(c.getTitle());
                        userContestInfoVO.setContestType(1);
                        int rank = oiContestRankVO.getRank() > 0 ? oiContestRankVO.getRank() : 0;
                        userContestInfoVO.setRank(rank);
                        userContestInfoVO.setBeatPercent(rank > 0 ? (double) (oiContestRankVOList.size() - rank + 1) / oiContestRankVOList.size() : 0.0);

                        // 构建oiStatistics其他部分
                        HashMap<String, Integer> submissionInfo = oiContestRankVO.getSubmissionInfo();
                        HashMap<String, Integer> timeInfo = oiContestRankVO.getTimeInfo();
                        for (int i = 0; i < pids.size(); i++) {
                            String pid = pids.get(i);
                            Integer score = submissionInfo.get(pid);
                            if (score != null) {
                                oiStatistics.get(i).setScore(submissionInfo.get(pid));
                                oiStatistics.get(i).setStatus(-1);
                            } else {
                                oiStatistics.get(i).setScore(0);
                                oiStatistics.get(i).setStatus(0);
                            }
                            if (timeInfo != null && timeInfo.get(pid) != null) {
                                oiStatistics.get(i).setStatus(1);
                                oiStatistics.get(i).setTime(timeInfo.get(pid));
                            } else {
                                oiStatistics.get(i).setTime(0);
                            }
                        }
                        Map<String,Integer> submitStatistic = new HashMap<>();
                        oiStatistics.forEach(oiStatistic -> {
                            if (oiStatistic.getStatus() == 1) {
                                submitStatistic.put("1",submitStatistic.getOrDefault("1",0) + 1);
                            } else if (oiStatistic.getStatus() == -1) {
                                if (oiStatistic.getScore() == 0) {
                                    submitStatistic.put("-1",submitStatistic.getOrDefault("-1",0) + 1);
                                } else {
                                    submitStatistic.put("0",submitStatistic.getOrDefault("0",0) + 1);
                                }
                            }
                        });

                        // 封装
                        OIStatisticVO oiStatisticVO = new OIStatisticVO();
                        oiStatisticVO.setPids(pids);
                        List<List<OIStatistic>> statistics = new ArrayList<>();
                        statistics.add(oiStatistics);
                        oiStatisticVO.setStatistics(statistics);

                        userContestInfoVO.setOIStatistic(oiStatisticVO);
                        userContestInfoVO.setSubmitStatistic(submitStatistic);
                        contestData.add(userContestInfoVO);
                        break;
                    }
                }
            }
            else if (records.get(0) instanceof ACMContestRankVO) {
                List<ACMContestRankVO> acmContestRankVOList = records;
                for (ACMContestRankVO acmContestRankVO : acmContestRankVOList) {
                    // 查询当前用户是否在排名中
                    if (acmContestRankVO.getUid().equals(userHomeInfo.getUid())) {
                        // 查比赛题目
                        List<ContestProblemVO> contestProblem = null;
                        try {
                            contestProblem = contestManager.getContestProblem(c.getId(), true);
                        } catch (StatusFailException | StatusForbiddenException e) {
                            throw new RuntimeException(e);
                        }
                        // pid
                        List<String> pids = contestProblem.stream().map(ContestProblemVO::getDisplayId).collect(Collectors.toList());
                        // 对应的acmStatistics
                        List<ACMStatistic> acmStatistics = contestProblem.stream().map(cp -> {
                            ACMStatistic acmStatistic = new ACMStatistic();
                            acmStatistic.setTitle(cp.getDisplayTitle());
                            acmStatistic.setDifficulty(pidDifficultyMap.get(cp.getPid()));
                            return acmStatistic;
                        }).collect(Collectors.toList());

                        // 根据比赛信息构建数据
                        userContestInfoVO.setOrder(index[0]++);
                        userContestInfoVO.setContestId(c.getId());
                        userContestInfoVO.setContestName(c.getTitle());
                        userContestInfoVO.setContestType(0);
                        int rank = acmContestRankVO.getRank() > 0 ? acmContestRankVO.getRank() : 0;
                        userContestInfoVO.setRank(rank);
                        userContestInfoVO.setBeatPercent(rank > 0 ? (double) (acmContestRankVOList.size() - rank + 1) / acmContestRankVOList.size() : 0.0);

                        // 构建acmStatistics其他部分
                        HashMap<String, HashMap<String, Object>> submissionInfo = acmContestRankVO.getSubmissionInfo();
                        for (int i = 0; i < pids.size(); i++) {
                            String pid = pids.get(i);
                            HashMap<String, Object> info = submissionInfo.get(pid);
                            int total = 0;
                            if (info != null) {
                                if (info.get("isAC") == null) {
                                    total = (int) info.get("errorNum") * (-1);
                                    acmStatistics.get(i).setTime(0);
                                } else {
                                    total = (int) info.get("errorNum") + 1;
                                    acmStatistics.get(i).setTime((int) (long) info.get("ACTime") / 60);
                                }
                                if (info.get("isFirstAC") != null) {
                                    if ((boolean) info.get("isFirstAC"))acmStatistics.get(i).setFirst(1);
                                    else acmStatistics.get(i).setFirst(0);
                                }
                            } else {
                                acmStatistics.get(i).setFirst(0);
                                acmStatistics.get(i).setTime(0);
                            }
                            acmStatistics.get(i).setTotal(total);
                        }
                        Map<String,Integer> submitStatistic = new HashMap<>();
                        acmStatistics.forEach(acmStatistic -> {
                            if (acmStatistic.getTotal() > 0) {
                                submitStatistic.put("1",submitStatistic.getOrDefault("1",0) + 1);
                            } else if (acmStatistic.getTotal() < 0) {
                                submitStatistic.put("0",submitStatistic.getOrDefault("0",0) + 1);
                            }
                        });

                        // 封装
                        ACMStatisticVO acmStatisticVO = new ACMStatisticVO();
                        acmStatisticVO.setPids(pids);
                        List<List<ACMStatistic>> statistics = new ArrayList<>();
                        statistics.add(acmStatistics);
                        acmStatisticVO.setStatistics(statistics);

                        userContestInfoVO.setACMStatistic(acmStatisticVO);
                        userContestInfoVO.setSubmitStatistic(submitStatistic);
                        contestData.add(userContestInfoVO);
                        break;
                    }
                }
            }
        });
        userHomeInfo.setContestData(contestData);
        // 未通过题目
        List<String> unsolvedProblems = new ArrayList<>();
        for (String displayPid : unsolvedMap.keySet()){
            if (unsolvedMap.get(displayPid) == 0) continue;
            unsolvedProblems.add(displayPid);
        }
        userHomeInfo.setUnsolvedList(unsolvedProblems);

        // 获取用户已通过题目的pid
        Collections.sort(pidList);

        // 获取所有题目对应的标签
        // 这里的数据由于上方代码逻辑需要重复使用,已移至上方

        // 获取已涉足(已通过的题目)标签tid
        List<Long> acTids = null;
        if (!CollUtil.isEmpty(pidList)) {
            List<ProblemTag> touchedTags = tagProblem.stream().filter(t -> pidList.contains(t.getPid())).collect(Collectors.toList());
            Map<Long,Long> tagIdStatistics = touchedTags.stream().collect(Collectors.groupingBy(ProblemTag::getTid, Collectors.counting()));

            // 标签统计
            Map<String,Long> tagStatistics = new HashMap<>();
            for (Tag t : allTagList) {
                if (tagIdStatistics.get(t.getId()) != null) {
                    tagStatistics.put(t.getName(),tagIdStatistics.get(t.getId()));
                }
            }
            tagDifficultyStatisticVO.setTagStatistics(tagStatistics);
            acTids = touchedTags.stream().map(ProblemTag::getTid).distinct().collect(Collectors.toList());
        }
        // 标签难度统计
        userHomeInfo.setTagDifficultyStatistic(tagDifficultyStatisticVO);


        // 获取用户未涉足的标签tid
        // 看了源码,使用ArrayList的话复杂度是O(n*n),这里标签总数不会很大,复杂度能接受,优化可以使用双指针
        List<Long> untouchedTids = allTagList.stream().map(Tag::getId).collect(Collectors.toList());
        if (!CollUtil.isEmpty(acTids)) untouchedTids.removeAll(acTids);

        // 用户未涉足的标签列表
        List<Tag> untouchedTags = new ArrayList<>();
        for (int i = 0, j = 0; i < untouchedTids.size() && j < allTagList.size(); j++, i++) {
            while (j < allTagList.size() && ! allTagList.get(j).getId().equals(untouchedTids.get(i))) j++;
            if (j >= allTagList.size()) break;
            untouchedTags.add(allTagList.get(j));
        }


        // 未涉足的标签和分类
        List<ProblemTagVO> problemTagVOList = new ArrayList<>();
        if (CollectionUtils.isEmpty(classificationList)) {
            ProblemTagVO problemTagVo = new ProblemTagVO();
            problemTagVo.setTagList(untouchedTags);
            problemTagVOList.add(problemTagVo);
        } else {
            classificationList.forEach(c -> {
                List<Tag> tags = new ArrayList<>();
                for (Tag t : untouchedTags) {
                    if (c.getId().equals(t.getTcid())) {
                        tags.add(t);
                    }
                }
                ProblemTagVO problemTagVO = new ProblemTagVO();
                problemTagVO.setTagList(tags);
                problemTagVO.setClassification(c);
                if (!tags.isEmpty())  problemTagVOList.add(problemTagVO);
            });
        }

        // 未分类
        List<Tag> tags = new ArrayList<>();
        for (Tag t : untouchedTags) {
            if (t.getTcid() == null) {
                tags.add(t);
            }
        }
        ProblemTagVO problemTagVO = new ProblemTagVO();
        problemTagVO.setTagList(tags);
        if (!tags.isEmpty()) problemTagVOList.add(problemTagVO);

        userHomeInfo.setUntouchedTags(problemTagVOList);

        return userHomeInfo;
    }

    private ModelDataVO getModelData(List<Tag> tagList, Map<String, Map<Integer, int[]>> tagDifficultyModelMap, Map<String, int[]> tagACRateModelMap) {
        ModelDataVO modelDataVO = new ModelDataVO();
        // y轴数据即为tagList的name
        List<String> yAxisData = tagList.stream().map(Tag::getName).collect(Collectors.toList());
        modelDataVO.setYaxisData(yAxisData);
        // 坐标数据
        List<ModelAxisData> data = new ArrayList<>();
        for (int i = 0; i < tagList.size(); i++) {
            Map<Integer, int[]> map = tagDifficultyModelMap.get(tagList.get(i).getName());
            int[] easy = map.get(0);
            int[] medium = map.get(1);
            int[] hard = map.get(2);
            int ac = easy[0] + medium[0] + hard[0];
            int num = easy[1] + medium[1] + hard[1];
            if (easy[0] > 0 && easy[1] > 0) data.add(new ModelAxisData(0, i, (double) easy[0] / easy[1]));
            if (medium[0] > 0 && medium[1] > 0) data.add(new ModelAxisData(1, i, (double) medium[0] / medium[1]));
            if (hard[0] > 0 && hard[1] > 0) data.add(new ModelAxisData(2, i, (double) hard[0] / hard[1]));

            int[] arr = tagACRateModelMap.get(tagList.get(i).getName());
            if (arr[1] > 0) {
                data.add(new ModelAxisData(3, i, (double) arr[1] / (arr[0] + arr[1])));
            }

            if (ac > 0 && num > 0) data.add(new ModelAxisData(4, i, (double) ac / num));
        }
        modelDataVO.setData(data);
        return modelDataVO;
    }

    private UserHomeProblemVO convertProblemVO(Problem problem) {
        return UserHomeProblemVO.builder()
                .problemId(problem.getProblemId())
                .id(problem.getId())
                .difficulty(problem.getDifficulty())
                .build();
    }

    /**
     * @param uid
     * @param username
     * @return
     * @Description 获取用户最近一年的提交热力图数据
     */
    public UserCalendarHeatmapVO getUserCalendarHeatmap(String uid, String username) throws StatusFailException {
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        if (StringUtils.isEmpty(uid) && StringUtils.isEmpty(username)) {
            if (userRolesVo != null) {
                uid = userRolesVo.getUid();
            } else {
                throw new StatusFailException("请求参数错误：uid和username不能都为空！");
            }
        }
        UserCalendarHeatmapVO userCalendarHeatmapVo = new UserCalendarHeatmapVO();
        userCalendarHeatmapVo.setEndDate(DateUtil.format(new Date(), "yyyy-MM-dd"));
        List<Judge> lastYearUserJudgeList = userRecordEntityService.getLastYearUserJudgeList(uid, username);
        if (CollectionUtils.isEmpty(lastYearUserJudgeList)) {
            userCalendarHeatmapVo.setDataList(new ArrayList<>());
            return userCalendarHeatmapVo;
        }
        HashMap<String, Integer> tmpRecordMap = new HashMap<>();
        for (Judge judge : lastYearUserJudgeList) {
            Date submitTime = judge.getSubmitTime();
            String dateStr = DateUtil.format(submitTime, "yyyy-MM-dd");
            tmpRecordMap.merge(dateStr, 1, Integer::sum);
        }
        List<HashMap<String, Object>> dataList = new ArrayList<>();
        for (Map.Entry<String, Integer> record : tmpRecordMap.entrySet()) {
            HashMap<String, Object> tmp = new HashMap<>(2);
            tmp.put("date", record.getKey());
            tmp.put("count", record.getValue());
            dataList.add(tmp);
        }
        userCalendarHeatmapVo.setDataList(dataList);
        return userCalendarHeatmapVo;
    }


    /**
     * @MethodName changePassword
     * @Description 修改密码的操作，连续半小时内修改密码错误5次，则需要半个小时后才可以再次尝试修改密码
     * @Return
     * @Since 2021/1/8
     */
    public ChangeAccountVO changePassword(ChangePasswordDTO changePasswordDto) throws StatusSystemErrorException, StatusFailException {
        String oldPassword = changePasswordDto.getOldPassword();
        String newPassword = changePasswordDto.getNewPassword();

        // 数据可用性判断
        if (StringUtils.isEmpty(oldPassword) || StringUtils.isEmpty(newPassword)) {
            throw new StatusFailException("错误：原始密码或新密码不能为空！");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new StatusFailException("新密码长度应该为6~20位！");
        }
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        // 如果已经被锁定半小时，则不能修改
        String lockKey = Constants.Account.CODE_CHANGE_PASSWORD_LOCK + userRolesVo.getUid();
        // 统计失败的key
        String countKey = Constants.Account.CODE_CHANGE_PASSWORD_FAIL + userRolesVo.getUid();

        ChangeAccountVO resp = new ChangeAccountVO();
        if (redisUtils.hasKey(lockKey)) {
            long expire = redisUtils.getExpire(lockKey);
            Date now = new Date();
            long minute = expire / 60;
            long second = expire % 60;
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            resp.setCode(403);
            Date afterDate = new Date(now.getTime() + expire * 1000);
            String msg = "由于您多次修改密码失败，修改密码功能已锁定，请在" + minute + "分" + second + "秒后(" + formatter.format(afterDate) + ")再进行尝试！";
            resp.setMsg(msg);
            return resp;
        }
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.select("uuid", "password")
                .eq("uuid", userRolesVo.getUid());
        UserInfo userInfo = userInfoEntityService.getOne(userInfoQueryWrapper, false);
        // 与当前登录用户的密码进行比较判断
        if (userInfo.getPassword().equals(SecureUtil.md5(oldPassword))) { // 如果相同，则进行修改密码操作
            UpdateWrapper<UserInfo> updateWrapper = new UpdateWrapper<>();
            updateWrapper.set("password", SecureUtil.md5(newPassword))// 数据库用户密码全部用md5加密
                    .eq("uuid", userRolesVo.getUid());
            boolean isOk = userInfoEntityService.update(updateWrapper);
            if (isOk) {
                resp.setCode(200);
                resp.setMsg("修改密码成功！您将于5秒钟后退出进行重新登录操作！");
                // 清空记录
                redisUtils.del(countKey);
                return resp;
            } else {
                throw new StatusSystemErrorException("系统错误：修改密码失败！");
            }
        } else { // 如果不同，则进行记录，当失败次数达到5次，半个小时后才可重试
            Integer count = (Integer) redisUtils.get(countKey);
            if (count == null) {
                redisUtils.set(countKey, 1, 60 * 30); // 三十分钟不尝试，该限制会自动清空消失
                count = 0;
            } else if (count < 5) {
                redisUtils.incr(countKey, 1);
            }
            count++;
            if (count == 5) {
                redisUtils.del(countKey); // 清空统计
                redisUtils.set(lockKey, "lock", 60 * 30); // 设置锁定更改
            }
            resp.setCode(400);
            resp.setMsg("原始密码错误！您已累计修改密码失败" + count + "次...");
            return resp;
        }
    }


    public void getChangeEmailCode(String email) throws StatusFailException {

        String lockKey = Constants.Email.CHANGE_EMAIL_LOCK + email;
        if (redisUtils.hasKey(lockKey)) {
            throw new StatusFailException("对不起，您的操作频率过快，请在" + redisUtils.getExpire(lockKey) + "秒后再次发送修改邮件！");
        }

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        QueryWrapper<UserInfo> emailUserInfoQueryWrapper = new QueryWrapper<>();
        emailUserInfoQueryWrapper.select("uuid", "email")
                .eq("email", email);
        UserInfo emailUserInfo = userInfoEntityService.getOne(emailUserInfoQueryWrapper, false);

        if (emailUserInfo != null) {
            if (Objects.equals(emailUserInfo.getUuid(), userRolesVo.getUid())) {
                throw new StatusFailException("新邮箱与当前邮箱一致，请不要重复设置！");
            } else {
                throw new StatusFailException("该邮箱已被他人使用，请重新设置其它邮箱！");
            }
        }

        String numbers = RandomUtil.randomNumbers(6); // 随机生成6位数字的组合
        redisUtils.set(Constants.Email.CHANGE_EMAIL_KEY_PREFIX.getValue() + email, numbers, 10 * 60); //默认验证码有效10分钟
        emailManager.sendChangeEmailCode(email, userRolesVo.getUsername(), numbers);
        redisUtils.set(lockKey, 0, 30);
    }


    /**
     * @MethodName changeEmail
     * @Description 修改邮箱的操作，连续半小时内密码错误5次，则需要半个小时后才可以再次尝试修改
     * @Return
     * @Since 2021/1/9
     */
    public ChangeAccountVO changeEmail(ChangeEmailDTO changeEmailDto) throws StatusSystemErrorException, StatusFailException {

        String password = changeEmailDto.getPassword();
        String newEmail = changeEmailDto.getNewEmail();
        String code = changeEmailDto.getCode();
        // 数据可用性判断
        if (StringUtils.isEmpty(password) || StringUtils.isEmpty(newEmail) || StringUtils.isEmpty(code)) {
            throw new StatusFailException("错误：密码、新邮箱或验证码不能为空！");
        }

        if (!Validator.isEmail(newEmail)) {
            throw new StatusFailException("邮箱格式错误！");
        }

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        // 如果已经被锁定半小时不能修改
        String lockKey = Constants.Account.CODE_CHANGE_EMAIL_LOCK + userRolesVo.getUid();
        // 统计失败的key
        String countKey = Constants.Account.CODE_CHANGE_EMAIL_FAIL + userRolesVo.getUid();

        ChangeAccountVO resp = new ChangeAccountVO();
        if (redisUtils.hasKey(lockKey)) {
            long expire = redisUtils.getExpire(lockKey);
            Date now = new Date();
            long minute = expire / 60;
            long second = expire % 60;
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            resp.setCode(403);
            Date afterDate = new Date(now.getTime() + expire * 1000);
            String msg = "由于您多次修改邮箱失败，修改邮箱功能已锁定，请在" + minute + "分" + second + "秒后(" + formatter.format(afterDate) + ")再进行尝试！";
            resp.setMsg(msg);
            return resp;
        }

        QueryWrapper<UserInfo> emailUserInfoQueryWrapper = new QueryWrapper<>();
        emailUserInfoQueryWrapper.select("uuid", "email")
                .eq("email", changeEmailDto.getNewEmail());
        UserInfo emailUserInfo = userInfoEntityService.getOne(emailUserInfoQueryWrapper, false);

        if (emailUserInfo != null) {
            if (Objects.equals(emailUserInfo.getUuid(), userRolesVo.getUid())) {
                throw new StatusFailException("新邮箱与当前邮箱一致，请不要重复设置！");
            } else {
                throw new StatusFailException("该邮箱已被他人使用，请重新设置其它邮箱！");
            }
        }

        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.select("uuid", "password")
                .eq("uuid", userRolesVo.getUid());
        UserInfo userInfo = userInfoEntityService.getOne(userInfoQueryWrapper, false);

        String cacheCodeKey = Constants.Email.CHANGE_EMAIL_KEY_PREFIX.getValue() + newEmail;
        String cacheCode = (String) redisUtils.get(cacheCodeKey);
        if (cacheCode == null) {
            throw new StatusFailException("修改邮箱验证码不存在或已过期，请重新发送！");
        }

        if (!Objects.equals(cacheCode, code)) {
            Integer count = (Integer) redisUtils.get(countKey);
            if (count == null) {
                redisUtils.set(countKey, 1, 60 * 30); // 三十分钟不尝试，该限制会自动清空消失
                count = 0;
            } else if (count < 5) {
                redisUtils.incr(countKey, 1);
            }
            count++;
            if (count == 5) {
                redisUtils.del(countKey); // 清空统计
                redisUtils.set(lockKey, "lock", 60 * 30); // 设置锁定更改
            }

            resp.setCode(400);
            resp.setMsg("验证码错误！您已累计修改邮箱失败" + count + "次...");
            return resp;
        }

        // 与当前登录用户的密码进行比较判断
        if (userInfo.getPassword().equals(SecureUtil.md5(password))) { // 如果相同，则进行修改操作
            UpdateWrapper<UserInfo> updateWrapper = new UpdateWrapper<>();
            updateWrapper.set("email", newEmail)
                    .eq("uuid", userRolesVo.getUid());

            boolean isOk = userInfoEntityService.update(updateWrapper);
            if (isOk) {

                UserRolesVO userRoles = userRoleEntityService.getUserRoles(userRolesVo.getUid(), null);
                UserInfoVO userInfoVo = new UserInfoVO();

                BeanUtil.copyProperties(userRoles, userInfoVo, "roles");
                userInfoVo.setRoleList(userRoles
                        .getRoles()
                        .stream()
                        .map(Role::getRole)
                        .collect(Collectors.toList()));
                resp.setCode(200);
                resp.setMsg("修改邮箱成功！");
                resp.setUserInfo(userInfoVo);
                // 清空记录
                redisUtils.del(countKey, cacheCodeKey);
                return resp;
            } else {
                throw new StatusSystemErrorException("系统错误：修改邮箱失败！");
            }
        } else { // 如果不同，则进行记录，当失败次数达到5次，半个小时后才可重试
            Integer count = (Integer) redisUtils.get(countKey);
            if (count == null) {
                redisUtils.set(countKey, 1, 60 * 30); // 三十分钟不尝试，该限制会自动清空消失
                count = 0;
            } else if (count < 5) {
                redisUtils.incr(countKey, 1);
            }
            count++;
            if (count == 5) {
                redisUtils.del(countKey); // 清空统计
                redisUtils.set(lockKey, "lock", 60 * 30); // 设置锁定更改
            }

            resp.setCode(400);
            resp.setMsg("密码错误！您已累计修改邮箱失败" + count + "次...");
            return resp;
        }
    }


    public UserInfoVO changeUserInfo(UserInfoVO userInfoVo) throws StatusFailException {

        commonValidator.validateContentLength(userInfoVo.getRealname(), "真实姓名",50);
        commonValidator.validateContentLength(userInfoVo.getNickname(), "昵称",20);
        commonValidator.validateContentLength(userInfoVo.getSignature(), "个性简介",65535);
        commonValidator.validateContentLength(userInfoVo.getBlog(), "博客", 255);
        commonValidator.validateContentLength(userInfoVo.getGithub(), "Github", 255);
        commonValidator.validateContentLength(userInfoVo.getSchool(), "学校", 100);
        commonValidator.validateContentLength(userInfoVo.getNumber(), "学号", 200);
        commonValidator.validateContentLength(userInfoVo.getCfUsername(), "Codeforces用户名", 255);

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        UpdateWrapper<UserInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("uuid", userRolesVo.getUid())
                .set("cf_username", userInfoVo.getCfUsername())
                .set("realname", userInfoVo.getRealname())
                .set("nickname", userInfoVo.getNickname())
                .set("signature", userInfoVo.getSignature())
                .set("blog", userInfoVo.getBlog())
                .set("gender", userInfoVo.getGender())
                .set("github", userInfoVo.getGithub())
                .set("school", userInfoVo.getSchool())
                .set("number", userInfoVo.getNumber());

        boolean isOk = userInfoEntityService.update(updateWrapper);

        if (isOk) {
            UserRolesVO userRoles = userRoleEntityService.getUserRoles(userRolesVo.getUid(), null);
            // 更新session
            BeanUtil.copyProperties(userRoles, userRolesVo);
            UserInfoVO userInfoVO = new UserInfoVO();
            BeanUtil.copyProperties(userRoles, userInfoVO, "roles");
            userInfoVO.setRoleList(userRoles.getRoles().stream().map(Role::getRole).collect(Collectors.toList()));
            return userInfoVO;
        } else {
            throw new StatusFailException("更新个人信息失败！");
        }

    }

    public UserAuthInfoVO getUserAuthInfo(){
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        //获取该用户角色所有的权限
        List<Role> roles = userRoleEntityService.getRolesByUid(userRolesVo.getUid());
        UserAuthInfoVO authInfoVO = new UserAuthInfoVO();
        authInfoVO.setRoles(roles.stream().map(Role::getRole).collect(Collectors.toList()));
        return authInfoVO;
    }
}