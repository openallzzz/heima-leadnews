package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 自媒体文章审核
     *
     * @param id 自媒体文章id
     */
    @Override
    public void autoScanWmNews(Integer id) {
        // 1. 查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if(wmNews == null) {
            throw new RuntimeException("WmNewsAutoScanServiceImpl - 文章不存在");
        }

        // 从内容中提取纯文本和图片
        Map<String, Object> textAndImages = handlerTextAndImages(wmNews);

        // 2. 审核文本内容


        // 3. 审核图片内容

        // 4. 审核成功，保存app端的相关文章数据
    }

    /**
     * 1. 从自媒体文章的内容中提取纯文本和图片
     * 2. 提取文章中的封面图片
     * @param wmNews
     * @return
     */
    private Map<String, Object> handlerTextAndImages(WmNews wmNews) {
        StringBuilder stringBuilder = new StringBuilder();
        List<String> images = new ArrayList<>();
        if(StringUtils.isBlank(wmNews.getContent())) {
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if(map.get("type").equals("text")) {
                    stringBuilder.append(map.get("value"));
                } else if(map.get("type").equals("image")) {
                    images.add((String) map.get("value"));
                } else {
                    throw new RuntimeException("内容含有暂未支持的片段哦");
                }
            }
        }

        if(StringUtils.isNotBlank(wmNews.getImages())) {
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", stringBuilder.toString());
        result.put("images", images);

        return result;
    }
}
