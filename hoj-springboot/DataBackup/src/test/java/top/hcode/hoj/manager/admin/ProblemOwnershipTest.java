package top.hcode.hoj.manager.admin;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.manager.admin.problem.AdminProblemManager;
import top.hcode.hoj.manager.admin.contest.AdminContestProblemManager;
import top.hcode.hoj.pojo.dto.ProblemDTO;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.validator.ProblemValidator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProblemOwnershipTest {
    private ProblemEntityService problems;
    private Subject subject;
    private Problem original;
    private ProblemDTO request;

    @BeforeEach
    void setUp() {
        subject = mock(Subject.class);
        AccountProfile profile = new AccountProfile();
        profile.setUsername("owner");
        when(subject.getPrincipal()).thenReturn(profile);
        ThreadContext.bind(subject);
        problems = mock(ProblemEntityService.class);
        original = new Problem().setId(1L).setProblemId("P1000").setAuthor("owner");
        when(problems.getById(1L)).thenReturn(original);
        when(problems.getOne(any())).thenReturn(original);
        when(problems.adminUpdateProblem(any())).thenReturn(true);
        request = new ProblemDTO();
        request.setProblem(new Problem().setId(1L).setProblemId("P1000").setAuthor("new-author"));
    }

    @AfterEach
    void tearDown() {
        ThreadContext.remove();
    }

    @Test
    void currentAuthorCanTransferPublicProblem() {
        AdminProblemManager manager = manager(new AdminProblemManager());
        assertDoesNotThrow(() -> manager.updateProblem(request));
        verify(problems).adminUpdateProblem(request);
    }

    @Test
    void currentAuthorCanTransferContestProblem() {
        AdminContestProblemManager manager = manager(new AdminContestProblemManager());
        assertDoesNotThrow(() -> manager.updateProblem(request));
        verify(problems).adminUpdateProblem(request);
    }

    @Test
    void forgedRequestAuthorDoesNotGrantPublicProblemAccess() {
        original.setAuthor("someone-else");
        request.getProblem().setAuthor("owner");
        AdminProblemManager manager = manager(new AdminProblemManager());
        assertThrows(StatusForbiddenException.class, () -> manager.updateProblem(request));
        verify(problems, never()).adminUpdateProblem(any());
    }

    @Test
    void forgedRequestAuthorDoesNotGrantContestProblemAccess() {
        original.setAuthor("someone-else");
        request.getProblem().setAuthor("owner");
        AdminContestProblemManager manager = manager(new AdminContestProblemManager());
        assertThrows(StatusForbiddenException.class, () -> manager.updateProblem(request));
        verify(problems, never()).adminUpdateProblem(any());
    }

    private <T> T manager(T manager) {
        ReflectionTestUtils.setField(manager, "problemEntityService", problems);
        ReflectionTestUtils.setField(manager, "problemValidator", mock(ProblemValidator.class));
        return manager;
    }
}
