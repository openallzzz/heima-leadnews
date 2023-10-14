package com.heima.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.service.ApArticleConfigService;
import com.heima.model.article.pojos.ApArticleConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class ApArticleConfigServiceImpl
        extends ServiceImpl<ApArticleConfigMapper, ApArticleConfig> implements ApArticleConfigService {


    /**
     * 修改文章
     *
     * @param map
     */
    @Override
    public void updateByMap(Map map) {
        if(map == null || map.size() != 2) {
            log.error("文章端修改下架字段时错误，原因：自媒体端传递参数无效");
            return ;
        }

        // enable 0 下架 1 上架
        boolean isDown = true;
        Object enable = map.get("enable");
        if(enable.equals(1)) {
            isDown = false;
        }

        // 修改文章配置的下架字段
        update(Wrappers.<ApArticleConfig>lambdaUpdate()
                .eq(ApArticleConfig::getArticleId, map.get("articleId"))
                .set(ApArticleConfig::getIsDown, isDown));
    }
}
