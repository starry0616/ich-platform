package com.yuxi.ich.user.controller;

import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.user.dto.ChangePasswordRequest;
import com.yuxi.ich.user.dto.UserProfileRequest;
import com.yuxi.ich.user.dto.UserTargetItem;
import com.yuxi.ich.user.entity.SysMenu;
import com.yuxi.ich.user.entity.SysUser;
import com.yuxi.ich.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户中心接口（对应说明书表 6-2 接口 5—10）。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 获取当前用户资料 */
    @GetMapping("/profile")
    public Result<SysUser> profile() {
        return Result.success(userService.getProfile());
    }

    /** 修改个人资料 */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileRequest request) {
        userService.updateProfile(request);
        return Result.success("保存成功", null);
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return Result.success("密码修改成功", null);
    }

    /** 更新头像（传入媒体地址，实际文件走 /api/media/upload） */
    @PostMapping("/avatar")
    public Result<Void> updateAvatar(@RequestBody Map<String, String> body) {
        userService.updateAvatar(body.get("avatar"));
        return Result.success("头像已更新", null);
    }

    /** 我的收藏列表 */
    @GetMapping("/favorites")
    public Result<PageResult<UserTargetItem>> favorites(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(userService.myFavorites(pageNum, pageSize));
    }

    /** 我的浏览历史 */
    @GetMapping("/history")
    public Result<PageResult<UserTargetItem>> history(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(userService.myHistory(pageNum, pageSize));
    }

    /** 当前角色菜单（前端动态导航） */
    @GetMapping("/menus")
    public Result<List<SysMenu>> menus() {
        return Result.success(userService.myMenus());
    }
}
