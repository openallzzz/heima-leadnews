package com.heima.wemedia.service.impl;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {


    /**
     * 查询文章列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
        // 1. 查询参数
        dto.checkParam();

        // 2. 分页条件查询
        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = new LambdaQueryWrapper();

        // 状态精确查询
        if(dto.getStatus() != null) {
            lambdaQueryWrapper.eq(WmNews::getStatus, dto.getStatus());
        }

        // 频道精确查询
        if(dto.getChannelId() != null) {
            lambdaQueryWrapper.eq(WmNews::getChannelId, dto.getChannelId());
        }

        // 时间范围查询
        if(dto.getBeginPubDate() != null && dto.getEndPubDate() != null) {
            lambdaQueryWrapper.between(WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate());
        }

        // 关键字的模糊查询
        if(StringUtils.isNotBlank(dto.getKeyword())) {
            lambdaQueryWrapper.like(WmNews::getTitle, dto.getKeyword());
        }

        // 当前登录用户的文章
        if(WmThreadLocalUtil.getUser() != null) {
            lambdaQueryWrapper.eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getId());
        } else {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        // 按照发布时间倒序查询
        lambdaQueryWrapper.orderByDesc(WmNews::getPublishTime);

        page = page(page, lambdaQueryWrapper);

        // 3. 结果返回
        ResponseResult result = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        result.setData(page.getRecords());

        return result;
    }

    /**
     * 文章发布、修改或者保存为草稿
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult submitNews(WmNewsDto dto) {
        // 0. 参数判断
        if(dto == null || dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 1. 保存或修改文章
        WmNews wmNews = new WmNews();
        // 属性拷贝
        BeanUtils.copyProperties(dto, wmNews);
        // List -> String
        if(dto.getImages() != null && dto.getImages().size() > 0) {
            String imageStr = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imageStr);
        }

        // 如果当前封面类型为自动 -1
        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            wmNews.setType(null);
        }
        
        saveOrUpdateWmNews(wmNews);

        // 2. 是草稿的情况

        // 3. 不是草稿，保存文章内容图片与素材的引用关系

        // 4. 不是草稿，保存文章封面图片与素材的引用关系
        return null;
    }

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    /**
     * 保存或修改文章
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        // 补全属性
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setPublishTime(new Date());
        wmNews.setEnable((short) 1); // 默认上架

        if(wmNews.getId() == null) {
            // 保存
            save(wmNews);
        } else {
            // 修改
            // 删除文章图片和素材的关系
            wmNewsMaterialMapper.delete(
                    Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }
    }
}
