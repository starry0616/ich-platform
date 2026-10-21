package com.yuxi.ich;

import com.yuxi.ich.media.entity.IchProjectMedia;
import com.yuxi.ich.media.mapper.IchProjectMediaMapper;
import com.yuxi.ich.media.service.MediaService;
import com.yuxi.ich.project.mapper.IchProjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 媒体生命周期纯单元测试（第 8B 批）。
 *
 * <p>不启动 Spring、不连数据库；用 Mockito 替换 Mapper，上传目录指向 JDK 临时目录，
 * 隔离验证「数据库入库失败时已落盘文件被补偿清理」。不写入项目现有 uploads。
 */
class MediaLifecycleUnitTests {

    @Test
    @DisplayName("U1. 数据库入库抛异常时，本次新写文件被补偿删除，原异常重抛且无残留")
    void dbInsertFailureCleansWrittenFile() throws Exception {
        IchProjectMediaMapper mediaMapper = Mockito.mock(IchProjectMediaMapper.class);
        IchProjectMapper projectMapper = Mockito.mock(IchProjectMapper.class);
        Path root = Files.createTempDirectory("media-compensate-");
        try {
            MediaService service = new MediaService(mediaMapper, projectMapper);
            Field dirField = MediaService.class.getDeclaredField("uploadDir");
            dirField.setAccessible(true);
            dirField.set(service, root.toString());

            // 可解码的小 PNG；入库阶段模拟数据库故障
            when(mediaMapper.insert(any(IchProjectMedia.class)))
                    .thenThrow(new RuntimeException("simulated db failure"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "comp.png", "image/png", tinyPng());

            assertThatThrownBy(() -> service.upload(file, null, null, "补偿测试"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("simulated db failure");

            // 补偿后临时目录内不应残留任何常规文件
            try (Stream<Path> walk = Files.walk(root)) {
                long files = walk.filter(Files::isRegularFile).count();
                assertThat(files).as("入库失败补偿后不应残留磁盘文件").isZero();
            }
        } finally {
            deleteRecursively(root);
        }
    }

    private byte[] tinyPng() throws Exception {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    private void deleteRecursively(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        }
    }
}
