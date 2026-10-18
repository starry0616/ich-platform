package com.yuxi.ich.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.project.entity.IchCategory;
import com.yuxi.ich.project.mapper.IchCategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 非遗类别服务。
 *
 * <p>类别是低频变更的字典数据，且前台每次筛选都要用到，
 * 适合加缓存。骨架阶段先直接查库，接入 Redis 时在本类加 @Cacheable 即可。
 */
@Service
public class CategoryService {

    private final IchCategoryMapper categoryMapper;

    public CategoryService(IchCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /** 查询全部启用状态的类别，按 sort_no 升序 */
    public List<IchCategory> listAll() {
        return categoryMapper.selectList(
                Wrappers.<IchCategory>lambdaQuery()
                        .eq(IchCategory::getStatus, 1)
                        .orderByAsc(IchCategory::getSortNo));
    }
}
