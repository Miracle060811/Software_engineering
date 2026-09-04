package com.travelmate.microservices.local;

import com.travelmate.entity.ReviewReport;
import com.travelmate.entity.Review;
import com.travelmate.entity.Reply;
import com.travelmate.mapper.ReviewMapper;
import com.travelmate.mapper.ReplyMapper;
import com.travelmate.mapper.ReviewReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalAdminLocalControllerTests {
    private ReviewReportMapper mapper;
    private ReviewMapper reviewMapper;
    private ReplyMapper replyMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mapper = mock(ReviewReportMapper.class);
        reviewMapper = mock(ReviewMapper.class);
        replyMapper = mock(ReplyMapper.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new InternalAdminLocalController(mapper, reviewMapper, replyMapper, "shared-token")).build();
    }

    @Test
    void adminCanDeleteReportedReviewAndManageReplies() throws Exception {
        ReviewReport report = new ReviewReport(); report.setId(3L); report.setReviewId(8L); report.setStatus(0);
        Review review = new Review(); review.setId(8L); review.setDeleted(0);
        Reply reply = new Reply(); reply.setId(9L); reply.setReviewId(8L); reply.setDeleted(0);
        when(mapper.selectById(3L)).thenReturn(report);
        when(reviewMapper.selectById(8L)).thenReturn(review);
        when(replyMapper.selectList(any())).thenReturn(List.of(reply));
        when(replyMapper.selectById(9L)).thenReturn(reply);

        mockMvc.perform(post("/internal/local/admin/review-reports/3/delete-review")
                        .header("X-Internal-Token", "shared-token").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/internal/local/admin/reviews/8/replies")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(9));
        mockMvc.perform(post("/internal/local/admin/reviews/8/replies")
                        .header("X-Internal-Token", "shared-token").contentType("application/json")
                        .content("{\"content\":\"感谢反馈\",\"adminId\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("感谢反馈"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/internal/local/admin/replies/9").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk());

        verify(reviewMapper).updateById(review);
        verify(replyMapper).updateById(reply);
    }

    @Test
    void adminLocalEndpointsReturnReportsResolutionAndCount() throws Exception {
        ReviewReport report = new ReviewReport(); report.setId(3L); report.setStatus(0);
        when(mapper.selectList(any())).thenReturn(List.of(report));
        when(mapper.selectById(3L)).thenReturn(report);
        when(mapper.selectCount(any())).thenReturn(1L);
        mockMvc.perform(get("/internal/local/admin/review-reports").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(3));
        mockMvc.perform(post("/internal/local/admin/review-reports/3/resolve")
                        .header("X-Internal-Token", "shared-token").contentType("application/json")
                        .content("{\"remark\":\"已复核\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(1));
        mockMvc.perform(post("/internal/local/admin/review-reports/3/reject")
                        .header("X-Internal-Token", "shared-token").contentType("application/json")
                        .content("{\"remark\":\"举报不成立\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(1));
        mockMvc.perform(get("/internal/local/admin/pending-report-count").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(1));
    }

    @Test
    void adminLocalEndpointsRejectInvalidInternalToken() throws Exception {
        mockMvc.perform(get("/internal/local/admin/review-reports").header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/local/admin/review-reports/3/resolve")
                        .header("X-Internal-Token", "wrong-token").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/local/admin/review-reports/3/reject")
                        .header("X-Internal-Token", "wrong-token").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/internal/local/admin/pending-report-count").header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminLocalEndpointsRejectMalformedRequests() throws Exception {
        mockMvc.perform(get("/internal/local/admin/review-reports").param("status", "bad")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/internal/local/admin/review-reports/not-a-number/resolve")
                        .header("X-Internal-Token", "shared-token").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/internal/local/admin/review-reports/not-a-number/reject")
                        .header("X-Internal-Token", "shared-token").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/internal/local/admin/pending-report-count"))
                .andExpect(status().isBadRequest());
    }
}
