package com.yuxi.ich.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.admin.entity.SysBanner;
import com.yuxi.ich.admin.mapper.SysBannerMapper;
import com.yuxi.ich.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页推荐位前台接口（公开轮播）。
 */
@RestController
@RequestMapping("/api/banner")
public class BannerController {

    private final SysBannerMapper bannerMapper;

    public BannerController(SysBannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    /** 启用中的推荐位（按排序） */
    @GetMapping("/list")
    public Result<List<SysBanner>> list() {
        List<SysBanner> banners = bannerMapper.selectList(
                Wrappers.<SysBanner>lambdaQuery()
                        .eq(SysBanner::getStatus, 1)
                        .orderByAsc(SysBanner::getSortNo));
        return Result.success(banners);
    }
}
