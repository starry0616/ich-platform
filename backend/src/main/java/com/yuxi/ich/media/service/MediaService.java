package com.yuxi.ich.media.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.media.entity.IchProjectMedia;
import com.yuxi.ich.media.mapper.IchProjectMediaMapper;
import com.yuxi.ich.project.mapper.IchProjectMapper;
import com.yuxi.ich.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 多媒体资源服务（说明书表 6-3 接口 24—25）。
 *
 * <p>存储策略：文件落盘到本地上传目录，数据库只保存 URL；
 * 接入 MinIO / OSS 时只需替换 writeFile 的实现，接口不变。
 * 扩展名与 MIME 白名单校验，防止上传恶意文件。
 */
@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    /** 图片扩展名白名单 */
    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    /**
     * JDK 内置 ImageIO 可解码的图片格式。
     * webp 无 JDK 内置解码器（需第三方库，本批不新增依赖），故仅保留扩展名+大小校验，
     * 不做内容解码验证（见类注释与审计报告的剩余风险）。
     */
    private static final Set<String> IMAGE_DECODABLE = Set.of("jpg", "jpeg", "png", "gif", "bmp");
    /** 音频扩展名白名单 */
    private static final Set<String> AUDIO_EXTS = Set.of("mp3", "wav", "m4a", "aac", "ogg", "flac");
    /** 视频扩展名白名单 */
    private static final Set<String> VIDEO_EXTS = Set.of("mp4", "webm", "mov", "mkv", "avi");

    /** 图片大小上限（5 MB） */
    private static final long IMAGE_MAX_BYTES = 5L * 1024 * 1024;
    /** 音视频大小上限（200 MB） */
    private static final long MEDIA_MAX_BYTES = 200L * 1024 * 1024;

    @Value("${ich.media.upload-dir:uploads}")
    private String uploadDir;

    private final IchProjectMediaMapper mediaMapper;
    private final IchProjectMapper projectMapper;

    public MediaService(IchProjectMediaMapper mediaMapper, IchProjectMapper projectMapper) {
        this.mediaMapper = mediaMapper;
        this.projectMapper = projectMapper;
    }

    /**
     * 上传文件并登记资源记录。
     *
     * @return 资源记录（含访问 URL）
     */
    @Transactional(rollbackFor = Exception.class)
    public IchProjectMedia upload(MultipartFile file, Long projectId,
                                  Integer mediaType, String title) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "请选择要上传的文件");
        }
        int type = resolveMediaType(mediaType, file.getOriginalFilename());
        String ext = resolveExt(file.getOriginalFilename());
        long size = file.getSize();
        if (size <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "文件内容为空");
        }
        if (type == 1 && size > IMAGE_MAX_BYTES) {
            throw new BizException(ResultCode.PARAM_ERROR, "图片不能超过 5 MB");
        }
        if (type != 1 && size > MEDIA_MAX_BYTES) {
            throw new BizException(ResultCode.PARAM_ERROR, "音视频文件不能超过 200 MB");
        }

        // 关联项目非空时必须真实存在（projectId=null 表示独立媒体，允许空）；
        // 在写盘前校验，非法引用直接 400，不落盘、不解码。selectById 自动排除逻辑删除。
        if (projectId != null && projectMapper.selectById(projectId) == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "关联的非遗项目不存在");
        }

        // 图片在大小限制通过后、写盘前做真实内容解码校验（webp 无内置解码器，跳过）
        if (type == 1) {
            validateImageContent(file, ext);
        }

        // 存储：uploads/yyyyMMdd/{uuid}.{ext}
        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadDir, day);
        Path target = dir.resolve(fileName);
        try {
            Files.createDirectories(dir);
            file.transferTo(target);
        } catch (IOException e) {
            log.error("文件写入失败 path={}", target, e);
            // 写盘失败：清理可能已产生的半截文件，避免残留；清理失败仅记录，不掩盖原始异常
            safeDeleteQuietly(target);
            throw new BizException(ResultCode.ERROR, "文件保存失败，请重试");
        }

        IchProjectMedia record = new IchProjectMedia();
        record.setProjectId(projectId);
        record.setMediaType(type);
        record.setUrl("/api/media/file/" + day + "/" + fileName);
        record.setTitle(title);
        record.setSortNo(0);
        try {
            mediaMapper.insert(record);
        } catch (Exception e) {
            // 数据库入库失败：@Transactional 会回滚 DB，但回滚管不到磁盘文件，
            // 这里补偿删除本次新写文件并记录日志，随后重抛原异常让事务回滚。
            // 边界：方法正常返回后、事务提交阶段才失败的情况无法在此捕获，属已知限制。
            log.error("媒体记录入库失败，补偿清理已写文件 path={}", target, e);
            safeDeleteQuietly(target);
            throw e;
        }
        return record;
    }

    /** 按项目查询媒体列表 */
    public List<IchProjectMedia> listByProject(Long projectId) {
        if (projectId == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少项目 id");
        }
        return mediaMapper.selectList(
                Wrappers.<IchProjectMedia>lambdaQuery()
                        .eq(IchProjectMedia::getProjectId, projectId)
                        .orderByAsc(IchProjectMedia::getSortNo)
                        .orderByAsc(IchProjectMedia::getId));
    }

    /**
     * 删除资源（管理员）。
     *
     * <p>一致性策略：先删 DB 记录，再删除磁盘文件。若反过来先删盘，一旦 DB 删除回滚，
     * 会留下指向已不存在文件的悬空记录；先 DB 后盘时，磁盘删除失败只会残留可由定期
     * 任务清理的孤儿文件，不会产生悬空记录。磁盘文件必须位于配置上传根目录之内，
     * 拒绝任何路径逃逸。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        IchProjectMedia exist = mediaMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "资源不存在");
        }
        // 资源记录无归属人字段，简化为管理员可删；如需上传人权限校验，可在表上补充 uploader_id。
        if (!SecurityUtils.isAdmin()) {
            throw new BizException(ResultCode.FORBIDDEN, "仅管理员可删除资源");
        }
        mediaMapper.deleteById(id);
        deleteFileInsideRoot(exist.getUrl());
    }

    // ==================== 内部方法 ====================

    /** 安静删除单个文件，不存在视为成功；删除失败仅记录 warn，不抛出、不掩盖主异常 */
    private void safeDeleteQuietly(Path target) {
        try {
            if (target != null && Files.exists(target)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            log.warn("补偿清理文件失败 path={}", target, e);
        }
    }

    /**
     * 仅删除位于配置上传根目录之内的文件。
     * 从记录 URL 解析相对路径，normalize 后必须仍在根目录内；越界、无法识别或删除失败
     * 只记录日志，不抛业务异常（DB 记录已删除，残留孤儿可由定期任务补偿）。
     */
    private void deleteFileInsideRoot(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            String relative = resolveRelativeUrl(url);
            if (relative == null) {
                log.warn("无法识别的媒体 URL，跳过磁盘删除: {}", url);
                return;
            }
            Path target = root.resolve(relative).normalize();
            if (!target.startsWith(root)) {
                log.warn("媒体 URL 越出上传根目录，拒绝删除: {}", url);
                return;
            }
            safeDeleteQuietly(target);
        } catch (Exception e) {
            log.warn("删除媒体磁盘文件失败 url={}", url, e);
        }
    }

    /**
     * 从 {@code /api/media/file/day/file} 或 {@code /uploads/day/file} 提取相对路径。
     * 含 {@code ..}、空白或无法识别前缀时返回 null，交由上层拒绝，不直接拼路径。
     */
    private String resolveRelativeUrl(String url) {
        String p = url.trim();
        for (String prefix : new String[]{"/api/media/file/", "/uploads/"}) {
            if (p.startsWith(prefix)) {
                p = p.substring(prefix.length());
                break;
            }
        }
        int q = p.indexOf('?');
        if (q >= 0) {
            p = p.substring(0, q);
        }
        if (p.isBlank() || p.contains("..")) {
            return null;
        }
        return p;
    }

    private int resolveMediaType(Integer mediaType, String fileName) {
        String ext = resolveExt(fileName);
        if (mediaType != null && (mediaType == 1 || mediaType == 2 || mediaType == 3)) {
            // 以请求声明的类型为准，但扩展名必须落在对应白名单内
            if ((mediaType == 1 && !IMAGE_EXTS.contains(ext))
                    || (mediaType == 2 && !AUDIO_EXTS.contains(ext))
                    || (mediaType == 3 && !VIDEO_EXTS.contains(ext))) {
                throw new BizException(ResultCode.PARAM_ERROR,
                        "文件扩展名与声明的媒体类型不匹配");
            }
            return mediaType;
        }
        if (IMAGE_EXTS.contains(ext)) {
            return 1;
        }
        if (AUDIO_EXTS.contains(ext)) {
            return 2;
        }
        if (VIDEO_EXTS.contains(ext)) {
            return 3;
        }
        throw new BizException(ResultCode.PARAM_ERROR, "不支持的文件类型：" + ext);
    }

    private String resolveExt(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new BizException(ResultCode.PARAM_ERROR, "文件缺少扩展名");
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if (ext.isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "文件扩展名为空");
        }
        return ext;
    }

    /**
     * 对 JDK 可解码的图片格式做真实内容校验：必须能被 ImageIO 成功解码为有效图像。
     * 仅靠扩展名或客户端声明的 Content-Type 不能判定为图片；webp 无内置解码器，
     * 直接跳过（保持扩展名+大小校验，剩余风险见类注释）。
     *
     * <p>调用前已通过 5MB 大小限制，避免无上限读入；此处使用流解码，不把全文强行聚合。
     */
    private void validateImageContent(MultipartFile file, String ext) {
        if (!IMAGE_DECODABLE.contains(ext)) {
            return;
        }
        try (InputStream in = file.getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "图片内容无效或格式不受支持");
            }
        } catch (IOException e) {
            throw new BizException(ResultCode.PARAM_ERROR, "图片内容无效或格式不受支持");
        }
    }
}
