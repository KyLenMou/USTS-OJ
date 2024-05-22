package top.hcode.hoj.schedule;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateFormatUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import top.hcode.hoj.dao.common.FileEntityService;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.dao.msg.AdminSysNoticeEntityService;
import top.hcode.hoj.dao.msg.UserSysNoticeEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.dao.user.SessionEntityService;
import top.hcode.hoj.dao.user.UserInfoEntityService;
import top.hcode.hoj.dao.user.UserRecordEntityService;
import top.hcode.hoj.manager.msg.AdminNoticeManager;
import top.hcode.hoj.pojo.entity.common.File;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.msg.AdminSysNotice;
import top.hcode.hoj.pojo.entity.msg.UserSysNotice;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.user.Session;
import top.hcode.hoj.pojo.entity.user.UserInfo;
import top.hcode.hoj.pojo.entity.user.UserRecord;
import top.hcode.hoj.service.admin.rejudge.RejudgeService;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.JsoupUtils;
import top.hcode.hoj.utils.RedisUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


/**
 * 一个cron表达式有至少6个（也可能7个）有空格分隔的时间元素。按顺序依次为：
 * <p>
 * 字段	允许值	允许的特殊字符
 * 秒	0~59	, - * /
 * 分	0~59	, - * /
 * 小时	0~23	, - * /
 * 日期	1-31	, - * ? / L W C
 * 月份	1~12或者JAN~DEC	, - * /
 * 星期	1~7或者SUN~SAT	, - * ? / L C #
 * 年（可选）	留空，1970~2099	, - * /
 * <p>
 * “*”  字符代表所有可能的值
 * “-”  字符代表数字范围 例如1-5
 * “/”  字符用来指定数值的增量
 * “？” 字符仅被用于天（月）和天（星期）两个子表达式，表示不指定值。
 * 当2个子表达式其中之一被指定了值以后，为了避免冲突，需要将另一个子表达式的值设为“？”
 * “L” 字符仅被用于天（月）和天（星期）两个子表达式，它是单词“last”的缩写
 * 如果在“L”前有具体的内容，它就具有其他的含义了。
 * “W” 字符代表着平日(Mon-Fri)，并且仅能用于日域中。它用来指定离指定日的最近的一个平日。
 * 大部分的商业处理都是基于工作周的，所以 W 字符可能是非常重要的。
 * "C" 代表“Calendar”的意思。它的意思是计划所关联的日期，如果日期没有被关联，则相当于日历中所有日期。
 */
@Service
@Slf4j(topic = "hoj")
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private FileEntityService fileEntityService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private UserInfoEntityService userInfoEntityService;

    @Autowired
    private UserRecordEntityService userRecordEntityService;

    @Resource
    private SessionEntityService sessionEntityService;

    @Resource
    private AdminSysNoticeEntityService adminSysNoticeEntityService;

    @Resource
    private UserSysNoticeEntityService userSysNoticeEntityService;

    @Resource
    private JudgeEntityService judgeEntityService;

    @Resource
    private RejudgeService rejudgeService;

    @Resource
    private ProblemEntityService problemEntityService;

    @Resource
    private AdminNoticeManager adminNoticeManager;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private OtherContestService otherContestService;

    /**
     * @MethodName deleteAvatar
     * @Params * @param null
     * @Description 每天3点定时查询数据库字段并删除未引用的头像
     * @Return
     * @Since 2021/1/13
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Override
    public void deleteAvatar() {
        List<File> files = fileEntityService.queryDeleteAvatarList();
        // 如果查不到，直接结束
        if (files.isEmpty()) {
            return;
        }
        List<Long> idLists = new LinkedList<>();
        for (File file : files) {
            if (file.getDelete()) {
                boolean delSuccess = FileUtil.del(file.getFilePath());
                if (delSuccess) {
                    idLists.add(file.getId());
                }
            }
        }

        boolean isSuccess = fileEntityService.removeByIds(idLists);
        if (!isSuccess) {
            log.error("数据库file表删除头像数据失败----------------->sql语句执行失败");
        }
    }


    /**
     * @MethodName deleteTestCase
     * @Params * @param null
     * @Description 每天3点定时删除指定文件夹的上传测试数据
     * @Return
     * @Since 2021/2/7
     */
    @Scheduled(cron = "0 0 3 * * *")
//    @Scheduled(cron = "0/5 * * * * *")
    @Override
    public void deleteTestCase() {
        boolean result = FileUtil.del(Constants.File.TESTCASE_TMP_FOLDER.getPath());
        if (!result) {
            log.error("每日定时任务异常------------------------>{}", "清除本地的题目测试数据失败!");
        }
    }

    /**
     * @MethodName deleteContestPrintText
     * @Params * @param null
     * @Description 每天4点定时删除本地的比赛打印数据
     * @Return
     * @Since 2021/9/19
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Override
    public void deleteContestPrintText() {
        boolean result = FileUtil.del(Constants.File.CONTEST_TEXT_PRINT_FOLDER.getPath());
        if (!result) {
            log.error("每日定时任务异常------------------------>{}", "清除本地的比赛打印数据失败!");
        }
    }

    /**
     * 每两小时获取其他OJ的比赛列表，并保存在redis里
     * 保存格式：
     * oj: "Codeforces",
     * title: "Codeforces Round #680 (Div. 1, based on VK Cup 2020-2021 - Final)",
     * beginTime: "2020-11-08T05:00:00Z",
     * url: xxx
     */
    @Scheduled(cron = "0 0 0/2 * * *")
    @Override
    public void getOjContestsList() {
        // 开线程池
        ExecutorService executorService = Executors.newFixedThreadPool(7);
        // 将获取的比赛列表添加进这里
        List<Map<String, Object>> contestsList = new ArrayList<>();
        // 获取其他OJ的比赛列表
        List<Callable<List<Map<String, Object>>>> callableList = new ArrayList<>();
        callableList.add(otherContestService.getNowcoderContestsList());
        callableList.add(otherContestService.getLuoguContestsList());
        callableList.add(otherContestService.getCodeforcesContestsList());
        callableList.add(otherContestService.getAcWingContestsList());
        callableList.add(otherContestService.getAtCoderContestsList());
        callableList.add(otherContestService.getLeetCodeContestsList());
        try {
            List<Future<List<Map<String, Object>>>> futures = executorService.invokeAll(callableList);
            for (Future<List<Map<String, Object>>> future : futures) {
                contestsList.addAll(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("获取其他OJ的比赛列表失败！----------------------->{}", e.getMessage());
        } finally {
            executorService.shutdown();
        }
        // 把比赛列表按照开始时间排序，方便查看
        contestsList.sort((o1, o2) -> {
            long beginTime1 = ((Date) o1.get("beginTime")).getTime();
            long beginTime2 = ((Date) o2.get("beginTime")).getTime();
            return Long.compare(beginTime1, beginTime2);
        });

        // 获取对应的redis key
        String redisKey = Constants.Schedule.RECENT_OTHER_CONTEST.getCode();
        // 缓存时间一天
        redisUtils.set(redisKey, contestsList, 60 * 60 * 24);
    }
    /**
     * @author: KyLen
     * @date: 2024/4/16 下午12:47
     * @param: []
     * @description: 每天零点零一分获取力扣每日一题
     **/
    @Scheduled(cron = "0 1 0 * * *")
    public void getLeetCodeDailyProblem() {
        // Create a RestTemplate object
        RestTemplate restTemplate = new RestTemplate();

        // Set request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Set request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", "\n query CalendarTaskSchedule($days: Int!) {\n calendarTaskSchedule(days: $days) {\n contests {\n id\n name\n slug\n progress\n link\n premiumOnly\n }\n dailyQuestions {\n id\n name\n slug\n progress\n link\n premiumOnly\n }\n studyPlans {\n id\n name\n slug\n progress\n link\n premiumOnly\n }\n }\n}\n ");
        requestBody.put("variables", new HashMap<String, Object>() {{
            put("days", 0);
        }});
        requestBody.put("operationName", "CalendarTaskSchedule");

        // Send HTTP POST request and get response
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("https://leetcode.cn/graphql", requestEntity, String.class);

        // Parse JSON data
        JSONObject json = new JSONObject(response.getBody());
        String link = json.getJSONObject("data").getJSONObject("calendarTaskSchedule").getJSONArray("dailyQuestions").getJSONObject(0).getStr("link");

        Map<String, String> dailyQuestionMap = new HashMap<>();
        dailyQuestionMap.put("oj", "LeetCode");
        dailyQuestionMap.put("url", link);
        List<Map<String,String>> list = new ArrayList<>();
        list.add(dailyQuestionMap);
        redisUtils.set(Constants.Schedule.Daily_Problem.getCode(), list, 60 * 60 * 24);
    }

    /**
     * 每隔一个小时获取nowcoder的rating分数
     */
    // @Scheduled(cron = "0 0 3 * * *")
    @Scheduled(cron = "0 0 * * * *")
    @Override
    public void getNowcoderRating() {
        String nowcoderAPI = "https://ac.nowcoder.com/acm/contest/profile/";
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        // 查询nowcoder_id不为空的数据
        userInfoQueryWrapper.isNotNull("nowcoder_id");
        List<UserInfo> userInfoList = userInfoEntityService.list(userInfoQueryWrapper);
        for (UserInfo userInfo : userInfoList) {
            // 获取牛客id
            String nowcoderId = userInfo.getNowcoderId();
            log.info("正在获取用户 {}({}:{})的nowcoder分数",userInfo.getUsername(),userInfo.getRealname(),nowcoderId);
            // 获取uuid
            String uuid = userInfo.getUuid();
            // 格式化api
            String ratingAPI = nowcoderAPI + nowcoderId;
            Integer tempRating = -1;
            try {
                Document doc = Jsoup.connect(ratingAPI).get();
                Elements elements = doc.select("div[class^=state-num rate-score]");
                if (!elements.isEmpty()) {
                    String ratingStr = elements.first().text();
                    Integer rating = Integer.parseInt(ratingStr);
                    tempRating = rating;
                    // 更新数据库
                    UpdateWrapper<UserRecord> userRecordUpdateWrapper = new UpdateWrapper<>();
                    userRecordUpdateWrapper.eq("uid", uuid).set("nowcoder_rating", rating);
                    boolean result = userRecordEntityService.update(userRecordUpdateWrapper);
                    log.info("获取用户 {}({}:{})的nowcoder分数成功----------->{}",userInfo.getUsername(),userInfo.getRealname(),nowcoderId,tempRating);
                    if (!result) {
                        log.error("(nowcoder)更新UserRecord表失败");
                    }
                }
            } catch (Exception e) {
                log.error("获取用户 {}({}:{})的nowcoder分数异常----------------------->{}",userInfo.getUsername(),userInfo.getRealname(),nowcoderId,e.getMessage());
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 每隔一个小时获取cf的rating分数
     */
    // @Scheduled(cron = "0 0 3 * * *")
    @Scheduled(cron = "0 0 * * * *")
    @Override
    public void getCodeforcesRating() {
        String codeforcesUserInfoAPI = "https://mirror.codeforces.com/api/user.info?handles=%s";
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        // 查询cf_username不为空的数据
        userInfoQueryWrapper.isNotNull("cf_username");
        List<UserInfo> userInfoList = userInfoEntityService.list(userInfoQueryWrapper);
        for (UserInfo userInfo : userInfoList) {
            // 获取cf名字
            String cfUsername = userInfo.getCfUsername();
            log.info("正在获取用户 {}({}:{})的cf分数",userInfo.getUsername(),userInfo.getRealname(),cfUsername);
            // 获取uuid
            String uuid = userInfo.getUuid();
            // 格式化api
            String ratingAPI = String.format(codeforcesUserInfoAPI, cfUsername);
            Integer tempRating = -1;
            try {
                // 连接api，获取json格式对象
                // ScheduleServiceImpl service = applicationContext.getBean(ScheduleServiceImpl.class);
                // JSONObject resultObject = service.getCFUserInfo(ratingAPI);

                // 使用restTemplate
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<String> response = restTemplate.getForEntity(ratingAPI, String.class);
                JSONObject resultObject = new JSONObject(response.getBody());

                // 获取状态码
                String status = resultObject.getStr("status");
                // 如果查无此用户，则跳过
                if ("FAILED".equals(status)) {
                    continue;
                }
                // 用户信息存放在result列表中的第0个
                JSONObject cfUserInfo = resultObject.getJSONArray("result").getJSONObject(0);
                // 获取cf的分数
                Integer cfRating = cfUserInfo.getInt("rating", null);
                tempRating = cfRating;
                UpdateWrapper<UserRecord> userRecordUpdateWrapper = new UpdateWrapper<>();
                // 将对应的cf分数修改
                userRecordUpdateWrapper.eq("uid", uuid).set("rating", cfRating);
                boolean result = userRecordEntityService.update(userRecordUpdateWrapper);
                log.info("获取用户 {}({}:{})的cf分数成功----------->{}",userInfo.getUsername(),userInfo.getRealname(),cfUsername,tempRating);
                if (!result) {
                    log.error("插入UserRecord表失败------------------------------->");
                }

            } catch (Exception e) {
                log.error("获取用户 {}({}:{})的cf分数异常----------------------->{}",userInfo.getUsername(),userInfo.getRealname(),cfUsername,e.getMessage());
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Retryable(value = Exception.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 1.4))
    public JSONObject getCFUserInfo(String url) throws Exception {
        return JsoupUtils.getJsonFromConnection(JsoupUtils.getConnectionFromUrl(url, null, null));
    }


    /**
     * @MethodName deleteUserSession
     * @Params * @param null
     * @Description 每天3点定时删除用户半年的session表记录
     * @Return
     * @Since 2021/9/6
     */
    @Scheduled(cron = "0 0 3 * * *")
//    @Scheduled(cron = "0/5 * * * * *")
    @Override
    public void deleteUserSession() {
        QueryWrapper<Session> sessionQueryWrapper = new QueryWrapper<>();
        DateTime dateTime = DateUtil.offsetMonth(new Date(), -6);
        String strTime = DateFormatUtils.format(dateTime, "yyyy-MM-dd HH:mm:ss");
        sessionQueryWrapper.select("distinct uid");
        sessionQueryWrapper.apply("UNIX_TIMESTAMP(gmt_create) >= UNIX_TIMESTAMP('" + strTime + "')");
        List<Session> sessionList = sessionEntityService.list(sessionQueryWrapper);
        if (sessionList.size() > 0) {
            List<String> uidList = sessionList.stream().map(Session::getUid).collect(Collectors.toList());
            QueryWrapper<Session> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("uid", uidList)
                    .apply("UNIX_TIMESTAMP('" + strTime + "') > UNIX_TIMESTAMP(gmt_create)");
            List<Session> needDeletedSessionList = sessionEntityService.list(queryWrapper);
            if (needDeletedSessionList.size() > 0) {
                List<Long> needDeletedIdList = needDeletedSessionList.stream().map(Session::getId).collect(Collectors.toList());
                boolean isOk = sessionEntityService.removeByIds(needDeletedIdList);
                if (!isOk) {
                    log.error("=============数据库session表定时删除用户6个月前的记录失败===============");
                }
            }
        }
    }


    /**
     * @MethodName syncNoticeToUser
     * @Description 每一小时拉取系统通知表admin_sys_notice到表user_sys_notice(只推送给半年内有登录过的用户)
     * @Return
     * @Since 2021/10/3
     */
    @Override
    @Scheduled(cron = "0 0 0/1 * * *")
    public void syncNoticeToRecentHalfYearUser() {
        QueryWrapper<AdminSysNotice> adminSysNoticeQueryWrapper = new QueryWrapper<>();
        adminSysNoticeQueryWrapper.eq("state", false);
        List<AdminSysNotice> adminSysNotices = adminSysNoticeEntityService.list(adminSysNoticeQueryWrapper);
        if (adminSysNotices.size() == 0) {
            return;
        }

        QueryWrapper<Session> sessionQueryWrapper = new QueryWrapper<>();
        sessionQueryWrapper.select("DISTINCT uid");
        List<Session> sessionList = sessionEntityService.list(sessionQueryWrapper);
        List<String> userIds = sessionList.stream().map(Session::getUid).collect(Collectors.toList());

        for (AdminSysNotice adminSysNotice : adminSysNotices) {
            switch (adminSysNotice.getType()) {
                case "All":
                    List<UserSysNotice> userSysNoticeList = new ArrayList<>();
                    for (String uid : userIds) {
                        UserSysNotice userSysNotice = new UserSysNotice();
                        userSysNotice.setRecipientId(uid)
                                .setType("Sys")
                                .setSysNoticeId(adminSysNotice.getId());
                        userSysNoticeList.add(userSysNotice);
                    }
                    boolean isOk1 = userSysNoticeEntityService.saveOrUpdateBatch(userSysNoticeList);
                    if (isOk1) {
                        adminSysNotice.setState(true);
                    }
                    break;
                case "Single":
                    UserSysNotice userSysNotice = new UserSysNotice();
                    userSysNotice.setRecipientId(adminSysNotice.getRecipientId())
                            .setType("Mine")
                            .setSysNoticeId(adminSysNotice.getId());
                    boolean isOk2 = userSysNoticeEntityService.saveOrUpdate(userSysNotice);
                    if (isOk2) {
                        adminSysNotice.setState(true);
                    }
                    break;
                case "Admin":
                    break;
            }

        }

        boolean isUpdateNoticeOk = adminSysNoticeEntityService.saveOrUpdateBatch(adminSysNotices);
        if (!isUpdateNoticeOk) {
            log.error("=============推送系统通知更新状态失败===============");
        }

    }

    @Override
    @Scheduled(cron = "0 0/20 * * * ?")
    public void check20MPendingSubmission() {
        DateTime dateTime = DateUtil.offsetMinute(new Date(), -15);
        String strTime = DateFormatUtils.format(dateTime, "yyyy-MM-dd HH:mm:ss");

        QueryWrapper<Judge> judgeQueryWrapper = new QueryWrapper<>();
        judgeQueryWrapper.select("distinct submit_id");
        judgeQueryWrapper.eq("status", Constants.Judge.STATUS_PENDING.getStatus());
        judgeQueryWrapper.apply("UNIX_TIMESTAMP('" + strTime + "') > UNIX_TIMESTAMP(gmt_modified)");
        List<Judge> judgeList = judgeEntityService.list(judgeQueryWrapper);
        if (!CollectionUtils.isEmpty(judgeList)) {
            log.info("Half An Hour Check Pending Submission to Rejudge:" + Arrays.toString(judgeList.toArray()));
            for (Judge judge : judgeList) {
                rejudgeService.rejudge(judge.getSubmitId());
            }
        }
    }

    /**
     * 每天6点检查一次有没有处于正在申请中的团队题目申请公开的进度单子，发消息给超级管理和题目管理员
     */
    @Override
    @Scheduled(cron = "0 0 6 * * *")
//    @Scheduled(cron = "0/5 * * * * *")
    public void checkUnHandleGroupProblemApplyProgress() {
        QueryWrapper<Problem> problemQueryWrapper = new QueryWrapper<>();
        problemQueryWrapper.eq("apply_public_progress", 1).isNotNull("gid");
        int count = problemEntityService.count(problemQueryWrapper);
        if (count > 0) {
            String title = "团队题目审批通知(Group Problem Approval Notice)";
            String content = getDissolutionGroupContent(count);
            List<String> superAdminUidList = userInfoEntityService.getSuperAdminUidList();
            List<String> problemAdminUidList = userInfoEntityService.getProblemAdminUidList();
            if (!CollectionUtils.isEmpty(problemAdminUidList)) {
                superAdminUidList.addAll(problemAdminUidList);
            }
            adminNoticeManager.addSingleNoticeToBatchUser(null, superAdminUidList, title, content, "Sys");
        }
    }

    private String getDissolutionGroupContent(int count) {
        return "您好，尊敬的管理员，目前有**" + count +
                "**条团队题目正在申请公开的单子，请您尽快前往后台 [团队题目审批](/admin/group-problem/apply) 进行审批！"
                + "\n\n" +
                "Hello, dear administrator, there are currently **" + count
                + "** problem problems applying for public list. " +
                "Please go to the backstage [Group Problem Examine](/admin/group-problem/apply) for approval as soon as possible!";
    }

}
