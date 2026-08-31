package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.controller.AdminController;
import com.travelmate.entity.Review;
import com.travelmate.entity.ReviewReport;
import com.travelmate.entity.SysSensitiveWord;
import com.travelmate.mapper.ReviewMapper;
import com.travelmate.mapper.ReviewReportMapper;
import com.travelmate.mapper.SysSensitiveWordMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCase19AdminAuditWorkflowTests {
    private AdminController controller;
    private SysSensitiveWordMapper sensitiveWordMapper;
    private ReviewReportMapper reportMapper;
    private ReviewMapper reviewMapper;

    @BeforeAll
    static void initializeTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "use-case-19-test");
        TableInfoHelper.initTableInfo(assistant, ReviewReport.class);
        TableInfoHelper.initTableInfo(assistant, Review.class);
    }

    @BeforeEach
    void setUp() {
        controller = new AdminController();
        UserMapper userMapper = mock(UserMapper.class);
        sensitiveWordMapper = mock(SysSensitiveWordMapper.class);
        reportMapper = mock(ReviewReportMapper.class);
        reviewMapper = mock(ReviewMapper.class);
        User admin = new User();
        admin.setId(9L);
        admin.setUsername("admin-test");
        admin.setRole(1);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(admin);
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "sensitiveWordMapper", sensitiveWordMapper);
        ReflectionTestUtils.setField(controller, "reviewReportMapper", reportMapper);
        ReflectionTestUtils.setField(controller, "reviewMapper", reviewMapper);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin-test", null, "ROLE_ADMIN"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void intTc119CreatesTrimmedUniqueSensitiveWord() {
        when(sensitiveWordMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        SysSensitiveWord word = new SysSensitiveWord();
        word.setWord("  风险词  ");
        word.setLevel(2);
        Result<SysSensitiveWord> result = controller.addSensitiveWord(word);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(word.getWord()).isEqualTo("风险词");
        assertThat(word.getCreateTime()).isNotNull();
        verify(sensitiveWordMapper).insert(word);
    }

    @Test
    void unitTc119RejectsDuplicateSensitiveWord() {
        when(sensitiveWordMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        SysSensitiveWord word = new SysSensitiveWord();
        word.setWord("重复词");
        assertThatThrownBy(() -> controller.addSensitiveWord(word))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已存在");
        verify(sensitiveWordMapper, never()).insert(any(SysSensitiveWord.class));
    }

    @Test
    void intTc119ResolvesReportWithAuditRemark() {
        Result<Void> result = controller.resolveReviewReport(51L, Map.of("remark", "人工复核通过"));
        assertThat(result.getCode()).isEqualTo(200);
        verify(reportMapper).update(any(), any());
    }

    @Test
    void intTc119DeletesReportedReviewAndClosesReport() {
        ReviewReport report = new ReviewReport();
        report.setId(51L);
        report.setReviewId(66L);
        Review review = new Review();
        review.setId(66L);
        review.setDeleted(0);
        when(reportMapper.selectById(51L)).thenReturn(report);
        when(reviewMapper.selectById(66L)).thenReturn(review);
        Result<Void> result = controller.deleteReportedReview(51L, Map.of("remark", "违规评价已删除"));
        assertThat(result.getCode()).isEqualTo(200);
        verify(reviewMapper).update(any(), any());
        verify(reportMapper).update(any(), any());
    }
}
