package top.hcode.hoj.validator;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.dao.user.UserInfoEntityService;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.user.UserInfo;
import top.hcode.hoj.shiro.AccountProfile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProblemAuthorTest {
    private ProblemValidator validator;
    private UserInfoEntityService users;

    @BeforeEach
    void setUp() {
        validator = new ProblemValidator();
        users = mock(UserInfoEntityService.class);
        ReflectionTestUtils.setField(validator, "userInfoEntityService", users);
        AccountProfile profile = new AccountProfile();
        profile.setUsername("creator");
        Subject subject = mock(Subject.class);
        when(subject.getPrincipal()).thenReturn(profile);
        ThreadContext.bind(subject);
    }

    @AfterEach
    void tearDown() {
        ThreadContext.remove();
    }

    @Test
    void trimsAndResolvesRegisteredAccount() throws Exception {
        when(users.getOne(any())).thenReturn(new UserInfo().setUsername("Alice"));
        Problem problem = new Problem().setAuthor("  alice  ");
        validator.validateAuthor(problem, null);
        assertEquals("Alice", problem.getAuthor());
    }

    @Test
    void rejectsUnknownAccount() {
        assertThrows(StatusFailException.class,
                () -> validator.validateAuthor(new Problem().setAuthor("missing"), null));
    }

    @Test
    void newProblemsDefaultToCurrentAccount() throws Exception {
        when(users.getOne(any())).thenReturn(new UserInfo().setUsername("creator"));
        Problem problem = new Problem().setAuthor(" ");
        validator.validateAuthor(problem, null);
        assertEquals("creator", problem.getAuthor());
    }

    @Test
    void blankEditsAndUnchangedLegacyAuthorsPreserveOwnership() throws Exception {
        Problem original = new Problem().setAuthor("legacy-author");
        Problem edit = new Problem().setAuthor("");
        validator.validateAuthor(edit, original);
        assertEquals("legacy-author", edit.getAuthor());
        validator.validateAuthor(new Problem().setAuthor("legacy-author"), original);
        verifyZeroInteractions(users);
    }
}
