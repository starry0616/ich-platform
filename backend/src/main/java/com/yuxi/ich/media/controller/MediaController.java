package com.yuxi.ich.media.controller;

import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.media.entity.IchProjectMedia;
import com.yuxi.ich.media.service.MediaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 多媒体资源接口（对应说明书表 6-3 接口 24—25）。
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /** 上传图片 / 音视频资源（登录） */
    @PostMapping("/upload")
    public Result<IchProjectMedia> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "mediaType", required = false) Integer mediaType,
            @RequestParam(value = "title", required = false) String title) {
        return Result.success("上传成功", mediaService.upload(file, projectId, mediaType, title));
    }

    /** 项目的多媒体列表（公开） */
    @GetMapping("/list")
    public Result<List<IchProjectMedia>> list(@RequestParam("projectId") Long projectId) {
        return Result.success(mediaService.listByProject(projectId));
    }

    /** 删除资源（管理员） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        mediaService.delete(id);
        return Result.success("删除成功", null);
    }
}
