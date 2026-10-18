package com.yuxi.ich.media.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

/**
 * 项目多媒体资源表（ich_project_media）。
 *
 * <p>图片 / 音视频等非结构化资源与业务库解耦存储：
 * 数据库只保存 URL 与描述信息，文件本体位于对象存储或本地上传目录。
 */
@TableName("ich_project_media")
public class IchProjectMedia extends BaseEntity {

    /** 关联非遗项目 id */
    private Long projectId;

    /** 1 图片，2 音频，3 视频 */
    private Integer mediaType;

    /** 资源地址 */
    private String url;

    private String title;

    private Integer sortNo;

    /** 来源与授权说明 */
    private String source;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Integer getMediaType() {
        return mediaType;
    }

    public void setMediaType(Integer mediaType) {
        this.mediaType = mediaType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
