package top.hcode.hoj.manager.oj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.hcode.hoj.dao.contest.ContestRecordEntityService;
import top.hcode.hoj.dao.user.UserInfoEntityService;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.vo.ContestRecordVO;
import top.hcode.hoj.pojo.vo.OIContestRankVO;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OIScoreTimeRankTest {
    private ContestCalculateRankManager manager;
    private ContestRecordEntityService records;
    private Contest contest;

    @BeforeEach
    void setUp() {
        manager = new ContestCalculateRankManager();
        records = mock(ContestRecordEntityService.class);
        UserInfoEntityService users = mock(UserInfoEntityService.class);
        when(users.getSuperAdminUidList()).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(manager, "contestRecordEntityService", records);
        ReflectionTestUtils.setField(manager, "userInfoEntityService", users);
        contest = new Contest().setId(1L).setType(1).setStartTime(new Date(100000))
                .setEndTime(new Date(3700000)).setDuration(3600L)
                .setOiRankScoreType("Highest").setOiRankScoreTime(true).setAwardType(0);
    }

    @Test
    void usesLatestOfFirstBestScoresAndIgnoresRepeatedBestSubmissions() {
        List<OIContestRankVO> rank = rank(false,
                record("alice", "A", 100, 10, 1), record("alice", "B", 50, 100, 1),
                record("alice", "B", 50, 300, 1), record("bob", "A", 100, 60, 100),
                record("bob", "B", 50, 80, 100));
        assertEquals("bob", rank.get(0).getUsername());
        assertEquals(80L, rank.get(0).getScoreTime());
        assertEquals(100L, rank.get(1).getScoreTime());
        assertEquals(10L, rank.get(1).getScoreTimeInfo().get("A"));
        assertEquals(100L, rank.get(1).getScoreTimeInfo().get("B"));
        assertEquals(150, rank.get(1).getTotalScore());
    }

    @Test
    void mergedProblemScoresKeepTheFirstTimeOfTheHighestScore() {
        List<OIContestRankVO> rank = rank(false,
                record("alice", "A", 50, 10, 1), record("alice", "A", 100, 100, 1),
                record("alice", "A", 100, 200, 1), record("alice", "A", 50, 300, 1));
        assertEquals(100, rank.get(0).getTotalScore());
        assertEquals(100L, rank.get(0).getScoreTime());
        assertEquals(100L, rank.get(0).getScoreTimeInfo().get("A"));
    }

    @Test
    void zeroScoresHaveAttainmentTimesWithoutDelayingTheTotalScore() {
        List<OIContestRankVO> rank = rank(false,
                record("alice", "A", 100, 100, 5),
                record("alice", "B", 0, 300, 1), record("alice", "B", 0, 400, 1));
        assertEquals(300L, rank.get(0).getScoreTimeInfo().get("B"));
        assertEquals(100L, rank.get(0).getScoreTime());
        assertEquals(5, rank.get(0).getTotalTime());
    }

    @Test
    void recentScoringUsesFirstAttainmentOfTheDisplayedScore() {
        contest.setOiRankScoreType("Recent").setOiRankScoreTime(false);
        ContestRecordVO latest = record("alice", "A", 50, 300, 1);
        latest.setFirstScoreTime(80L);
        List<OIContestRankVO> rank = rank(false, latest);
        assertEquals(50, rank.get(0).getSubmissionInfo().get("A"));
        assertEquals(80L, rank.get(0).getScoreTimeInfo().get("A"));
    }

    @Test
    void recentZeroScoresRetainTheirFirstAttainmentTime() {
        contest.setOiRankScoreType("Recent").setOiRankScoreTime(false);
        ContestRecordVO latest = record("alice", "A", 0, 200, 1);
        latest.setFirstScoreTime(0L);
        List<OIContestRankVO> rank = rank(false, latest);
        assertEquals(0, rank.get(0).getSubmissionInfo().get("A"));
        assertEquals(0L, rank.get(0).getScoreTimeInfo().get("A"));
        assertEquals(0L, rank.get(0).getScoreTime());
    }

    @Test
    void submittedZeroScoresUseEarliestSubmissionInBothScoringModes() {
        for (String scoreType : Arrays.asList("Highest", "Recent")) {
            contest.setOiRankScoreType(scoreType).setOiRankScoreTime("Highest".equals(scoreType));
            ContestRecordVO latest = record("alice", "A", 0, 300, 1);
            latest.setFirstScoreTime(45L);
            OIContestRankVO row = rank(false, latest).get(0);
            assertEquals(0, row.getSubmissionInfo().get("A"));
            assertEquals(45L, row.getScoreTimeInfo().get("A"));
            assertEquals(0L, row.getScoreTime());
        }
    }

    @Test
    void unsubmittedProblemsHaveNeitherScoreNorTime() {
        for (String scoreType : Arrays.asList("Highest", "Recent")) {
            contest.setOiRankScoreType(scoreType).setOiRankScoreTime("Highest".equals(scoreType));
            OIContestRankVO row = rank(false, record("alice", "A", 100, 60, 1)).get(0);
            assertFalse(row.getSubmissionInfo().containsKey("B"));
            assertFalse(row.getScoreTimeInfo().containsKey("B"));
        }
    }

    @Test
    void sameScoreAndAttainmentTimeShareRankRegardlessOfExecutionTime() {
        List<OIContestRankVO> rank = rank(false,
                record("alice", "A", 100, 100, 1), record("bob", "A", 100, 100, 200));
        assertEquals(1, rank.get(0).getRank());
        assertEquals(1, rank.get(1).getRank());
    }

    @Test
    void legacyContestsStillUseExecutionTime() {
        contest.setOiRankScoreTime(null);
        List<OIContestRankVO> rank = rank(false,
                record("alice", "A", 100, 100, 10), record("bob", "A", 100, 50, 200));
        assertEquals("alice", rank.get(0).getUsername());
        assertEquals(2, rank.get(1).getRank());
    }

    @Test
    void totalScoreRemainsPrimaryAndZeroScoresStartAtZero() {
        List<OIContestRankVO> rank = rank(false,
                record("alice", "A", 100, 300, 1), record("bob", "A", 50, 10, 1),
                record("zero", "A", 0, 20, 1));
        assertEquals("alice", rank.get(0).getUsername());
        assertEquals(0L, rank.get(2).getScoreTime());
    }

    @Test
    void postContestSubmissionsOnlyCountWhenRequested() {
        ContestRecordVO during = record("alice", "A", 100, 100, 1);
        ContestRecordVO after = record("bob", "A", 100, 4000, 1);
        assertEquals(1, rank(false, during, after).size());
        assertEquals(2, rank(true, during, after).size());
    }

    @Test
    void starredUsersDoNotConsumeOfficialPlaces() {
        contest.setStarAccount("{\"star_account\":[\"alice\"]}");
        List<OIContestRankVO> rank = rank(false,
                record("alice", "A", 100, 10, 1), record("bob", "A", 100, 100, 1));
        assertEquals(-1, rank.get(0).getRank());
        assertEquals(1, rank.get(1).getRank());
    }

    private List<OIContestRankVO> rank(boolean includeAfter, ContestRecordVO... submissions) {
        when(records.getOIContestRecord(eq(contest), isNull(), eq(false), eq(includeAfter)))
                .thenReturn(Arrays.asList(submissions));
        return manager.calcOIRank(false, false, contest, null, null, null, false, null, includeAfter);
    }

    private ContestRecordVO record(String user, String problem, int score, long seconds, int runtime) {
        ContestRecordVO record = new ContestRecordVO();
        record.setUid(user);
        record.setUsername(user);
        record.setDisplayId(problem);
        record.setScore(score);
        record.setTime(seconds);
        record.setFirstScoreTime(seconds);
        record.setUseTime(runtime);
        record.setStatus(score == 100 ? 1 : 0);
        record.setSubmitTime(new Date(contest.getStartTime().getTime() + seconds * 1000));
        return record;
    }
}
