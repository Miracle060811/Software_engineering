package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.controller.ReviewReportController;
import com.travelmate.entity.Review;
import com.travelmate.entity.ReviewReport;
import com.travelmate.mapper.ReviewMapper;
import com.travelmate.mapper.ReviewReportMapper;
import com.travelmate.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UseCase09ReviewWorkflowTests {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unitTc109CreatesTrimmedReviewAndRejectsDuplicateOrder() {
        ReviewMapper mapper = mock(ReviewMapper.class);
        ReviewServiceImpl service = new ReviewServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L, 1L);
        when(mapper.insert(any(Review.class))).thenReturn(1);

        Review review = review(7L, 100L, "  房间很干净  ");
        service.addReview(review);

        assertThat(review.getContent()).isEqualTo("房间很干净");
        assertThat(review.getCreateTime()).isNotNull();
        verify(mapper).insert(review);

        assertThatThrownBy(() -> service.addReview(review(7L, 100L, "再次评价")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("该订单已评价");
    }

    @Test
    void unitTc109RejectsBlankContentAndInvalidRating() {
        ReviewServiceImpl service = new ReviewServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mock(ReviewMapper.class));

        assertThatThrownBy(() -> service.addReview(review(7L, 101L, "  ")))
                .hasMessage("评价内容不能为空");
        Review invalid = review(7L, 102L, "内容");
        invalid.setRating(6);
        assertThatThrownBy(() -> service.addReview(invalid))
                .hasMessage("评分必须在1到5之间");
    }

    @Test
    void intTc109CreatesReportForCurrentUserAndBlocksDuplicate() {
        ReviewReportController controller = new ReviewReportController();
        ReviewReportMapper reportMapper = mock(ReviewReportMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        User user = new User();
        user.setId(7L);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(reportMapper.selectCount(any(Wrapper.class))).thenReturn(0L, 1L);
        ReflectionTestUtils.setField(controller, "reportMapper", reportMapper);
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("reviewer", null, "ROLE_USER"));

        ReviewReport report = new ReviewReport();
        report.setReviewId(88L);
        report.setReason("虚假内容");
        Result<String> created = controller.report(report);

        assertThat(created.getCode()).isEqualTo(200);
        assertThat(report.getReporterId()).isEqualTo(7L);
        assertThat(report.getStatus()).isZero();
        assertThat(report.getCreateTime()).isNotNull();
        verify(reportMapper).insert(report);

        ReviewReport duplicate = new ReviewReport();
        duplicate.setReviewId(88L);
        assertThat(controller.report(duplicate).getMsg()).contains("已举报过");
    }

    private Review review(Long userId, Long orderId, String content) {
        Review review = new Review();
        review.setUserId(userId);
        review.setTargetId(1L);
        review.setTargetType(0);
        review.setOrderId(orderId);
        review.setRating(5);
        review.setContent(content);
        review.setDeleted(0);
        return review;
    }
}
