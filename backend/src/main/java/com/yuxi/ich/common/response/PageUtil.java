package com.yuxi.ich.common.response;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页转换工具：把 MyBatis-Plus 的 IPage 转成前端约定的 PageResult。
 */
public final class PageUtil {

    private PageUtil() {
    }

    /** 直接转换（实体与 VO 同类型时使用）。 */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getRecords(),
                page.getCurrent(), page.getSize());
    }

    /** 转换并逐条映射为 VO。 */
    public static <E, V> PageResult<V> of(IPage<E> page, Function<E, V> mapper) {
        List<V> list = page.getRecords().stream().map(mapper).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), list, page.getCurrent(), page.getSize());
    }
}
