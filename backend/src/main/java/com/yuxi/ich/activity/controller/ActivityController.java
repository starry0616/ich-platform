package com.yuxi.ich.activity.controller;

import com.yuxi.ich.activity.dto.ActivityQueryParam;
import com.yuxi.ich.activity.dto.ActivitySaveRequest;
import com.yuxi.ich.activity.dto.SignupRequest;
import com.yuxi.ich.activity.entity.ActSignup;
import com.yuxi.ich.activity.entity.IchActivity;
import com.yuxi.ich.activity.service.ActivityService;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 非遗活动与报名接口（对应说明书表 6-4 接口 30—37）。
 */
@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    // ==================== 公开查询 ====================

    /**
     * 活动列表（公开，仅已发布）。
     * <pre>
     * GET /api/activity/list?onlyOpen=true&amp;pageNum=1&amp;pageSize=10
     * </pre>
     */
    @GetMapping("/list")
    public Result<PageResult<IchActivity>> list(ActivityQueryParam query) {
        return Result.success(activityService.pageForFront(query));
    }

    /**
     * 活动详情（公开）。
     *
     * <p>返回体中带 remaining（剩余名额），前端据此控制按钮可用状态。
     */
    @GetMapping("/{id}")
    public Result<IchActivity> detail(@PathVariable("id") Long id) {
        return Result.success(activityService.getDetail(id, true));
    }

    // ==================== 报名 ====================

    /**
     * 活动报名。
     *
     * <pre>
     * POST /api/activity/1/signup
     * { "realName":"张三", "phone":"13800000000", "remark":"需要无障碍通道" }
     * </pre>
     *
     * <p>并发安全：内部用乐观锁 + 条件更新扣减名额，失败自动重试；
     * 名额满或重复报名返回 409。
     */
    @PostMapping("/{id}/signup")
    public Result<ActSignup> signup(@PathVariable("id") Long id,
                                    @Valid @RequestBody(required = false) SignupRequest request) {
        SignupRequest req = request == null ? new SignupRequest() : request;
        ActSignup signup = activityService.signup(id, req.getRealName(),
                req.getPhone(), req.getRemark());
        return Result.success("报名成功，请保存报名编号", signup);
    }

    /** 取消报名（报名截止前，且活动开始前 24 小时外） */
    @DeleteMapping("/{id}/signup")
    public Result<Void> cancelSignup(@PathVariable("id") Long id) {
        activityService.cancelSignup(id);
        return Result.success("已取消报名", null);
    }

    /** 我的报名列表 */
    @GetMapping("/my/signups")
    public Result<PageResult<ActSignup>> mySignups(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(activityService.mySignups(pageNum, pageSize));
    }

    // ==================== 主办方（传承人 / 管理员） ====================

    /**
     * 查看某活动的报名名单。
     *
     * <p>限主办传承人与管理员：角色由 {@code @PreAuthorize} 放行，
     * 具体归属（本人已认证档案主办 或 管理员）由 {@code ActivityService.checkActivityHost}
     * 在服务层比对，非主办传承人访问返回 403。
     */
    @GetMapping("/{id}/signups")
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<PageResult<ActSignup>> signupList(
            @PathVariable("id") Long id,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return Result.success(activityService.signupList(id, status, pageNum, pageSize));
    }

    /**
     * 现场核销。
     *
     * @param signupNo 报名编号（报名成功时返回给用户，现场出示）
     */
    @PutMapping("/{id}/checkin")
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<ActSignup> checkin(@PathVariable("id") Long id,
                                     @RequestParam("signupNo") String signupNo) {
        return Result.success("核销成功", activityService.checkin(id, signupNo));
    }

    /** 发布活动（提交审核） */
    @PostMapping
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<Long> create(@Valid @RequestBody ActivitySaveRequest request) {
        return Result.success("提交成功，等待审核", activityService.create(request));
    }

    /** 修改活动（修改后重新审核） */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody ActivitySaveRequest request) {
        request.setId(id);
        activityService.update(request);
        return Result.success("修改成功，等待审核", null);
    }

    /** 删除活动（逻辑删除，并级联清理互动记录） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        activityService.delete(id);
        return Result.success("删除成功", null);
    }

    // ==================== 后台管理 ====================

    /** 后台活动列表：含待审核（管理员） */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<IchActivity>> adminList(ActivityQueryParam query) {
        return Result.success(activityService.pageForAdmin(query));
    }

    /**
     * 活动审核（管理员）。
     *
     * @param status 1 通过（转为报名中），5 驳回（取消）
     */
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> audit(@PathVariable("id") Long id,
                              @RequestParam("status") Integer status,
                              @RequestParam(value = "remark", required = false) String remark) {
        activityService.audit(id, status, remark);
        return Result.success("审核完成", null);
    }
}
