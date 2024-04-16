package top.hcode.hoj.schedule;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @Author: KyLen
 * @Date: 2024/4/15 下午11:21
 * @Description:
 */
// @FunctionalInterface
public interface OtherContestService {
    Callable<List<Map<String, Object>>> getNowcoderContestsList();
    Callable<List<Map<String, Object>>> getLuoguContestsList();
    Callable<List<Map<String, Object>>> getCodeforcesContestsList();
    Callable<List<Map<String, Object>>> getAcWingContestsList();
    Callable<List<Map<String, Object>>> getAtCoderContestsList();
    Callable<List<Map<String, Object>>> getLeetCodeContestsList();
}
