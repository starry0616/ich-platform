package com.yuxi.ich;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 媒体上传内容校验与超限异常测试（第 8A 批）。
 *
 * <p>隔离：通过 {@link DynamicPropertySource} 把 {@code ich.media.upload-dir} 指向 JVM 临时目录，
 * 成功用例落盘到临时目录，不写入项目现有 uploads；使用 H2 内存库，不连接 MySQL。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "ich.media.upload-dir=target/test-uploads-isolated")
@DisplayName("媒体上传内容校验与超限异常测试")
class MediaUploadSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper om = new ObjectMapper();

    // ---------- 内容校验 ----------

    @Test
    @DisplayName("M1. 合法且可解码的小 PNG 上传成功(200)")
    void validDecodablePngAccepted() throws Exception {
        String token = loginToken("user");
        MockMultipartFile file = new MockMultipartFile(
                "file", "ok.png", "image/png", tinyPng());
        MvcResult r = mockMvc.perform(multipart("/api/media/upload").file(file)
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(200);
        assertThat(json.get("data").get("url").asText())
                .startsWith("/api/media/file/");
    }

    @Test
    @DisplayName("M2. 普通文本伪装为 .jpg 被拒绝(400)")
    void textFakeJpgRejected() throws Exception {
        String token = loginToken("user");
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg",
                "this is plain text, not an image".getBytes(StandardCharsets.UTF_8));
        MvcResult r = mockMvc.perform(multipart("/api/media/upload").file(file)
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("M3. 截断/损坏的图片内容改名为 .png 被拒绝(400)")
    void brokenPngContentRejected() throws Exception {
        String token = loginToken("user");
        // PNG 签名头后直接截断为残缺字节，ImageIO 无法解码
        byte[] broken = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01, 0x02};
        MockMultipartFile file = new MockMultipartFile(
                "file", "broken.png", "image/png", broken);
        MvcResult r = mockMvc.perform(multipart("/api/media/upload").file(file)
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("M4. 不在白名单的 .svg 被拒绝(400)")
    void svgRejected() throws Exception {
        assertRejectedByExtension("x.svg", "image/svg+xml",
                "<svg xmlns='http://www.w3.org/2000/svg'></svg>".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("M5. 不在白名单的 .html 被拒绝(400)")
    void htmlRejected() throws Exception {
        assertRejectedByExtension("x.html", "text/html",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("M6. 超过 5MB 的图片被拒绝(400)（大小校验先于解码）")
    void oversizedImageRejected() throws Exception {
        String token = loginToken("user");
        byte[] big = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", big);
        MvcResult r = mockMvc.perform(multipart("/api/media/upload").file(file)
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("M7. 未登录上传仍返回未授权(401)")
    void anonymousRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.png", "image/png", tinyPng());
        MvcResult r = mockMvc.perform(multipart("/api/media/upload").file(file)).andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(401);
    }

    // ---------- 超限异常处理器（在进入 Controller 前由解析层抛出） ----------

    @Test
    @DisplayName("M8. multipart 超限异常被统一转换为客户端 400，而非通用 500")
    void maxUploadSizeExceptionMappedTo400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var result = handler.handleMaxUploadSize(
                new MaxUploadSizeExceededException(200L * 1024 * 1024),
                new MockHttpServletRequest());
        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("上传文件超过大小限制");
    }

    // ---------- 工具 ----------

    private void assertRejectedByExtension(String filename, String contentType, byte[] content) throws Exception {
        String token = loginToken("user");
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, content);
        MvcResult r = mockMvc.perform(multipart("/api/media/upload").file(file)
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(400);
    }

    private byte[] tinyPng() throws IOException {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    private String loginToken(String username) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}")).andReturn();
        JsonNode b = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(b.get("code").asInt()).isEqualTo(200);
        return b.get("data").get("accessToken").asText();
    }
}
