package top.hcode.hoj.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TodoMapperTest {
    @Test
    void zeroScoresUseTheEarliestSubmissionRegardlessOfItsScore() {
        for (String method : Arrays.asList("getOIContestRecordByRecentSubmission", "getOIContestRecordByHighestSubmission")) {
            String sql = statement("ContestRecordMapper", method, oiParams()).getSql().replaceAll("\\s+", " ");
            assertTrue(sql.contains("CASE WHEN cr.score = 0 THEN"));
            assertTrue(sql.contains("MIN(first_submit.time)"));
            assertTrue(sql.contains("first_submit.cid = cr.cid"));
            assertTrue(sql.contains("first_submit.uid = cr.uid"));
            assertTrue(sql.contains("first_submit.pid = cr.pid"));
            assertTrue(sql.contains("first_submit.cpid = cr.cpid"));
            assertTrue(sql.contains("first_submit.time BETWEEN 0 AND cr.time"));
            assertFalse(sql.contains("first_submit.score ="));
        }
    }

    @Test
    void recentScoresLookUpTheFirstMatchingScoreWithinTheSelectedSubmissionWindow() {
        String sql = statement("ContestRecordMapper", "getOIContestRecordByRecentSubmission", oiParams())
                .getSql().replaceAll("\\s+", " ");
        assertTrue(sql.contains("MIN(first_score.time)"));
        assertTrue(sql.contains("first_score.cid = cr.cid"));
        assertTrue(sql.contains("first_score.score = cr.score"));
        assertTrue(sql.contains("first_score.status IS NOT NULL"));
        assertTrue(sql.contains("first_score.time BETWEEN 0 AND cr.time"));
    }

    @Test
    void contestantContestInfoIncludesTheSelectedTieBreaker() {
        Map<String, Object> params = new HashMap<>();
        params.put("cid", 1L);
        assertTrue(statement("ContestMapper", "getContestInfoById", params).getSql().contains("c.oi_rank_score_time"));
    }

    @Test
    void archivedFilterAndMemberVisibilityApplyBeforePagination() {
        Map<String, Object> params = new HashMap<>();
        params.put("archived", true);
        params.put("isRoot", false);
        params.put("uid", "member");
        params.put("onlyMine", true);
        params.put("auth", 0);
        BoundSql bound = statement("GroupMapper", "getGroupList", params);
        String sql = bound.getSql().replaceAll("\\s+", " ");
        assertTrue(sql.contains("g.archived = ?"));
        assertTrue(sql.contains("g.visible = true"));
        assertTrue(sql.contains("select uid from group_member where gid = g.id"));
        assertTrue(bound.getParameterMappings().stream().anyMatch(p -> "archived".equals(p.getProperty())));
    }

    @Test
    void sealedHighestScoreQueryFiltersBothTheAggregateAndJoinedRecords() {
        Map<String, Object> params = oiParams();
        params.put("isOpenSealRank", true);
        params.put("sealTime", 1200L);
        BoundSql bound = statement("ContestRecordMapper", "getOIContestRecordByHighestSubmission", params);
        String sql = bound.getSql().replaceAll("\\s+", " ");
        assertEquals(2, occurrences(sql, "cr.time BETWEEN 0 AND ?"));
        assertEquals(2, occurrences(sql, "cr.cid = ?"));
        assertTrue(sql.contains("ORDER BY cr.time ASC, cr.id ASC"));
    }

    @Test
    void mergedContestsUseScopedAliasesAndPermitAfterContestRecordsOnlyWhenRequested() {
        Map<String, Object> params = oiParams();
        params.put("externalCidList", Arrays.asList(2, 3));
        params.put("isContainsAfterContestJudge", true);
        String sql = statement("ContestRecordMapper", "getOIContestRecordByHighestSubmission", params)
                .getSql().replaceAll("\\s+", " ");
        assertEquals(2, occurrences(sql, "cr.time >= 0"));
        assertEquals(2, occurrences(sql, "cr.cid in"));
        assertTrue(sql.contains("FROM contest_record cr"));
    }

    private Map<String, Object> oiParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("cid", 1L);
        params.put("contestCreatorUid", "admin");
        params.put("isOpenSealRank", false);
        params.put("isContainsAfterContestJudge", false);
        params.put("endTime", 3600L);
        return params;
    }

    private BoundSql statement(String mapper, String method, Map<String, Object> params) {
        Configuration configuration = new Configuration();
        String resource = "top/hcode/hoj/mapper/xml/" + mapper + ".xml";
        InputStream xml = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull(xml);
        new XMLMapperBuilder(xml, configuration, resource, configuration.getSqlFragments()).parse();
        return configuration.getMappedStatement("top.hcode.hoj.mapper." + mapper + "." + method).getBoundSql(params);
    }

    private int occurrences(String text, String part) {
        return (text.length() - text.replace(part, "").length()) / part.length();
    }
}
