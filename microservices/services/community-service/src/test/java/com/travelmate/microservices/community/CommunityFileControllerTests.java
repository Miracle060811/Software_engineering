package com.travelmate.microservices.community;

import com.travelmate.controller.FileController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommunityFileControllerTests {
    @TempDir
    Path uploads;

    @Test
    void fileUploadReturnsNestedUrlContract() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FileController(uploads.toString())).build();
        byte[] png = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "trip.png", "image/png", png);
        mvc.perform(multipart("/api/file/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").isString());
    }

    @Test
    void fileUploadRejectsInvalidFiles() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FileController(uploads.toString())).build();
        MockMultipartFile file = new MockMultipartFile("file", "trip.txt", "text/plain", "bad".getBytes());
        mvc.perform(multipart("/api/file/upload").file(file))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
    }
}
