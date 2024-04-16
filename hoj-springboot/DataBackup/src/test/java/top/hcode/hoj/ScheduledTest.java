package top.hcode.hoj;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import top.hcode.hoj.schedule.OtherContestService;
import top.hcode.hoj.service.admin.rejudge.RejudgeService;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.JsoupUtils;
import top.hcode.hoj.utils.RedisUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @Author: KyLen
 * @Date: 2024/4/16 下午12:48
 * @Description:
 */
public class ScheduledTest {

    private static final String nowcoderContestAPI = "https://ac.nowcoder.com/acm/calendar/contest";
    private static final String luoGuContestAPI = "https://www.luogu.com.cn/contest/list?page=1&_contentOnly=1";
    private static final String codeForcesContestAPI = "https://codeforces.com/api/contest.list?gym=false";
    private static final String acWingContestAPI = "https://www.acwing.com/activity/1/competition";
    private static final String atCoderContestAPI = "https://atcoder.jp/contests";
    private static final String leetCodeContestAPI = "https://leetcode.cn/graphql";
    @org.junit.Test
    public void getOjContestsList() {
        // 开线程池
        ExecutorService executorService = Executors.newFixedThreadPool(7);
        // 将获取的比赛列表添加进这里
        List<Map<String, Object>> contestsList = new ArrayList<>();
        // 获取其他OJ的比赛列表
        List<Callable<List<Map<String, Object>>>> callableList = new ArrayList<>();
        callableList.add(getNowcoderContestsList());
        callableList.add(getLuoguContestsList());
        callableList.add(getCodeforcesContestsList());
        callableList.add(getAcWingContestsList());
        callableList.add(getAtCoderContestsList());
        callableList.add(getLeetCodeContestsList());
        try {
            List<Future<List<Map<String, Object>>>> futures = executorService.invokeAll(callableList);
            for (Future<List<Map<String, Object>>> future : futures) {
                contestsList.addAll(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
        } finally {
            executorService.shutdown();
        }
        // 把比赛列表按照开始时间排序，方便查看
        contestsList.sort((o1, o2) -> {
            long beginTime1 = ((Date) o1.get("beginTime")).getTime();
            long beginTime2 = ((Date) o2.get("beginTime")).getTime();
            return Long.compare(beginTime1, beginTime2);
        });
        contestsList.forEach(System.out::println);
    }
    // @Test
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
        System.out.println(list);
        // redisUtils.set(Constants.Schedule.Daily_Problem.getCode(), list, 60 * 60 * 24);
    }

    public Callable<List<Map<String, Object>>> getNowcoderContestsList() {
        return () -> {
            List<Map<String, Object>> contestsList = new ArrayList<>();
            // 获取当前年月
            DateTime dateTime = DateUtil.date();
            // offsetMonth 增加的月份，只枚举最近1个月的比赛
            for (int offsetMonth = 0; offsetMonth <= 1; offsetMonth++) {
                // 月份增加i个月
                DateTime newDate = DateUtil.offsetMonth(dateTime, offsetMonth);
                // 格式化API 月份从0-11，所以要加一
                String contestAPI = String.format(nowcoderContestAPI, newDate.year(), newDate.month() + 1);
                try {
                    // 连接api，获取json格式对象
                    JSONObject resultObject = JsoupUtils.getJsonFromConnection(JsoupUtils.getConnectionFromUrl(contestAPI, null, null));
                    // 比赛列表存放在data字段中
                    JSONArray contestsArray = resultObject.getJSONArray("data");
                    // 牛客比赛列表按时间顺序排序，所以从后向前取可以减少不必要的遍历
                    for (int i = contestsArray.size() - 1; i >= 0; i--) {
                        JSONObject contest = contestsArray.getJSONObject(i);
                        // 如果比赛已经结束了，则直接结束
                        if (contest.getLong("endTime", 0L) < dateTime.getTime()) {
                            break;
                        }
                        // 只要最近两周的比赛
                        if (contest.getLong("startTime") > dateTime.getTime() + 14 * 24 * 60 * 60 * 1000) {
                            continue;
                        }
                        // 把比赛列表信息添加在List里
                        contestsList.add(MapUtil.builder(new HashMap<String, Object>())
                                .put("oj", contest.getStr("ojName"))
                                .put("url", contest.getStr("link"))
                                .put("title", contest.getStr("contestName"))
                                .put("beginTime", new Date(contest.getLong("startTime")))
                                .put("endTime", new Date(contest.getLong("endTime"))).map());
                    }
                } catch (Exception e) {
                }
            }
            return contestsList;
        };
    }

    public Callable<List<Map<String, Object>>> getLuoguContestsList() {
        return () -> {
            List<Map<String, Object>> contestsList = new ArrayList<>();
            try {
                // 连接api，获取json格式对象
                JSONObject resultObject = JsoupUtils.getJsonFromConnection(JsoupUtils.getConnectionFromUrl(luoGuContestAPI, null, null));
                // 比赛列表存放在data字段中
                JSONArray contestsArray = resultObject.getJSONObject("currentData").getJSONObject("contests").getJSONArray("result");
                for (int i = 0; i < contestsArray.size(); i++) {
                    JSONObject contest = contestsArray.getJSONObject(i);
                    // 只要未开始的比赛
                    if (contest.getLong("startTime") < DateUtil.date().getTime() / 1000) {
                        continue;
                    }
                    // 把比赛列表信息添加在List里(时间戳为秒)
                    contestsList.add(MapUtil.builder(new HashMap<String, Object>())
                            .put("oj", "LuoGu")
                            .put("url", "https://www.luogu.com.cn/contest/" + contest.getStr("id"))
                            .put("title", contest.getStr("name"))
                            .put("beginTime", new Date(contest.getLong("startTime") * 1000))
                            .put("endTime", new Date(contest.getLong("endTime") * 1000)).map());
                }
            } catch (Exception e) {
            }
            return contestsList;
        };
    }

    public Callable<List<Map<String, Object>>> getCodeforcesContestsList() {
        return () -> {
            List<Map<String, Object>> contestsList = new ArrayList<>();
            try {
                // 连接api，获取json格式对象
                JSONObject resultObject = JsoupUtils.getJsonFromConnection(JsoupUtils.getConnectionFromUrl(codeForcesContestAPI, null, null));
                // 比赛列表存放在result字段中
                JSONArray contestsArray = resultObject.getJSONArray("result");
                for (int i = 0; i < contestsArray.size(); i++) {
                    JSONObject contest = contestsArray.getJSONObject(i);
                    if (contest.getStr("phase").equals("FINISHED")) {
                        break;
                    }
                    // 只要未开始的比赛和cf的比赛
                    if (contest.getLong("startTimeSeconds") < DateUtil.date().getTime() / 1000 || !contest.getStr("name").contains("Div")){
                        continue;
                    }
                    // 把比赛列表信息添加在List里(时间戳为秒)
                    contestsList.add(MapUtil.builder(new HashMap<String, Object>())
                            .put("oj", "CodeForces")
                            .put("url", "https://codeforces.com/contests/" + contest.getStr("id"))
                            .put("title", contest.getStr("name"))
                            .put("beginTime", new Date(contest.getLong("startTimeSeconds") * 1000))
                            .put("endTime", new Date(contest.getLong("startTimeSeconds") * 1000 + contest.getLong("durationSeconds") * 1000)).map());
                }
            } catch (Exception e) {
            }
            return contestsList;
        };
    }

    public Callable<List<Map<String, Object>>> getAcWingContestsList() {
        return () -> {
            List<Map<String, Object>> contestsList = new ArrayList<>();
            try {
                Document doc = Jsoup.connect(acWingContestAPI).get();
                Elements contests = doc.select(".activity-index-block");
                for (Element contest : contests) {
                    String status = contest.select(".activity_status").text();
                    if (!status.equals("未开始")) {
                        continue; // 如果不是未开始比赛，则跳过
                    }
                    String title = contest.select(".activity_title").text();
                    String startTimeStr = contest.select(".activity_td").get(1).text();
                    DateTime time = DateUtil.parse(startTimeStr);
                    Date start = new Date(time.getTime());

                    // 获取比赛链接
                    Element linkElement = contest.select("a").first();
                    String contestUrl = linkElement.attr("href");

                    // 将比赛信息存储到Map中
                    Map<String, Object> contestMap = new HashMap<>();
                    contestMap.put("oj", "AcWing");
                    contestMap.put("beginTime", start);
                    contestMap.put("title", "AcWing " + title);
                    contestMap.put("url", "https://www.acwing.com" + contestUrl);
                    contestsList.add(contestMap);
                }
            } catch (IOException e) {
            }

            return contestsList;
        };
    }

    public Callable<List<Map<String, Object>>> getAtCoderContestsList() {
        return () -> {
            List<Map<String, Object>> contestsList = new ArrayList<>();
            try {
                // 连接api，获取json格式对象
                Document doc = Jsoup.connect(atCoderContestAPI).get();
                // 比赛列表存放在result字段中
                Elements contests = doc.select("#contest-table-upcoming tbody tr");
                for (Element contest : contests) {
                    Map<String, Object> contestInfo = new HashMap<>();
                    Elements cols = contest.select("td");
                    // 提取比赛名称、时间和链接
                    String title = cols.get(1).select("a").text();
                    String startTime = cols.get(0).select("time").text();
                    String url = cols.get(1).select("a").attr("href");
                    DateTime parse = DateUtil.parse(startTime);
                    Date date = new Date(parse.getTime());
                    contestInfo.put("oj", "AtCoder");
                    contestInfo.put("title", title);
                    contestInfo.put("beginTime",date);
                    contestInfo.put("url", "https://atcoder.jp" + url);
                    contestsList.add(contestInfo);
                }
            } catch (Exception e) {
            }
            return contestsList;
        };
    }

    public Callable<List<Map<String, Object>>> getLeetCodeContestsList() {
        return () -> {
            List<Map<String, Object>> contestsList = new ArrayList<>();
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("query", "{\n  contestUpcomingContests {\n    containsPremium\n    title\n    cardImg\n    titleSlug\n    description\n    startTime\n    duration\n    originStartTime\n    isVirtual\n    isLightCardFontColor\n    company {\n      watermark\n      __typename\n    }\n    __typename\n  }\n}");

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(leetCodeContestAPI, requestEntity, String.class);


                JSONObject json = new JSONObject(response.getBody());
                JSONArray contests = json.getJSONObject("data").getJSONArray("contestUpcomingContests");

                // 遍历每个比赛并输出所需数据
                for (Object contest : contests) {
                    JSONObject contestJson = (JSONObject) contest;
                    long startTime = contestJson.getLong("startTime");
                    Date start = new Date(startTime * 1000);
                    long duration = contestJson.getLong("duration");
                    Date end = new Date(startTime + duration);
                    String title = contestJson.getStr("title");

                    Map<String, Object> contestMap = new HashMap<>();
                    contestMap.put("oj", "LeetCode");
                    contestMap.put("beginTime", start);
                    contestMap.put("endTime", end);
                    contestMap.put("title", "LeetCode " + title);
                    contestMap.put("url", "https://leetcode.cn/contest/" + contestJson.getStr("titleSlug"));

                    contestsList.add(contestMap);
                }
            } catch (Exception e) {
            }
            return contestsList;
        };
    }
}
