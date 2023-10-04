package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

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
    @Async // 文章审核应该是异步的，其本身没有返回值
    public void autoScanWmNews(Integer id) { // 走人工审核
        try {
            Thread.sleep(2000); // 文章审核之前的业务线还是比较长的，这里先等待一会
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 1. 查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            throw new RuntimeException("WmNewsAutoScanServiceImpl - 文章不存在");
        }

        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {

            // 从内容中提取纯文本和图片
            Map<String, Object> textAndImages = handlerTextAndImages(wmNews);

            // 自管理的敏感词过滤
            boolean isSensitive = handlerSensitiveScan((String) textAndImages.get("content"), wmNews);
            if (!isSensitive) {
                return;
            }

            // 2. 审核图片内容
            boolean isImageScan = handlerImageScan((List<String>) textAndImages.get("images"), wmNews);
            if (!isImageScan) {
                return;
            }

            // 3. 审核文本内容 （弃用阿里云的内容安全接口）
            // boolean isTextScan = handlerTextScan((String) textAndImages.get("content"), wmNews);
            // if (!isTextScan) {
            //     return;
            // }

            // 4. 审核成功，保存app端的相关文章数据
            ResponseResult result = saveAppArticle(wmNews);

            if (!result.getCode().equals(200)) {
                throw new RuntimeException("WmNewsAutoScanServiceImpl - 文章保存至app端失败");
            }

            wmNews.setArticleId((Long) result.getData());
            updateNewsStatus(wmNews, (short) 9, "审核成功");
        }
    }

    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;

    /**
     * 自管理的敏感词审核
     *
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handlerSensitiveScan(String content, WmNews wmNews) {
        // 获取所有的敏感词
        List<WmSensitive> wmSensitives =
                wmSensitiveMapper.selectList(Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives));
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());

        // 初始化敏感词库
        SensitiveWordUtil.initMap(sensitiveList);

        // 查看文章是否包含敏感词
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);

        if (map.size() > 0) {
            updateNewsStatus(wmNews, (short) 2, "当前文章中存在违规内容: " + map);
            return false;
        }

        return true;
    }

    @Qualifier("com.heima.apis.article.IArticleClient")
    @Autowired
    private IArticleClient articleClient;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 保存app端相关的文章数据
     *
     * @param wmNews
     */
    private ResponseResult saveAppArticle(WmNews wmNews) {

        ArticleDto dto = new ArticleDto();

        // 属性拷贝
        BeanUtils.copyProperties(wmNews, dto);

        // 文章布局
        dto.setLayout(wmNews.getType());

        // 频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if (wmChannel != null) {
            dto.setChannelName(wmChannel.getName());
        }

        // 作者
        dto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if (wmUser != null) {
            dto.setAuthorName(wmUser.getName());
        }

        // 设置文章id
        if (wmNews.getArticleId() != null) {
            dto.setId(wmNews.getArticleId());
        }
        dto.setCreatedTime(new Date());

        ResponseResult result = articleClient.saveArticle(dto);
        return result;
    }

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GreenImageScan greenImageScan;

    @Autowired
    private Tess4jClient tess4jClient;

    /**
     * 审核图片
     *
     * @param images
     * @param wmNews
     * @return
     */
    private boolean handlerImageScan(List<String> images, WmNews wmNews) {
        boolean flag = true;

        if (images == null || images.size() == 0) {
            return true;
        }

        // 下载图片到minio中，并去重
        images = images.stream().distinct().collect(Collectors.toList());

        // 下面的流程无法正常通过，因为本人并没有开启阿里云内容安全的付费服务
        // List<byte[]> imageList = new ArrayList<>(); // 阿里云审核图片使用到的
        try {
            for(String image : images) {
                byte[] bytes = fileStorageService.downLoadFile(image);

                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(in);
                // 识别图片中的文字
                String result = tess4jClient.doOCR(bufferedImage);
                // 过滤文字
                boolean isSensitive = handlerSensitiveScan(result, wmNews);
                if(!isSensitive) {
                    updateNewsStatus(wmNews, (short) 2, "当前文章中存在违规内容");
                    return false;
                }

                // imageList.add(bytes); // 阿里云审核使用图片使用到的
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        // 阿里云审核图片
        /*try {
            Map map = greenImageScan.imageScan(imageList);
            if(map != null) {
                if(map.get("suggestion").equals("block")) {
                    flag = false;
                    updateNewsStatus(wmNews, (short) 2, "当前文章中存在违规内容");
                }

                if(map.get("suggestion").equals("review")) {
                    flag = false;
                    updateNewsStatus(wmNews, (short) 3, "当前文章中存在不确定片段");
                }
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }*/

        return flag;
    }

    @Autowired
    private GreenTextScan greenTextScan;

    private boolean handlerTextScan(String content, WmNews wmNews) {
        boolean flag = true;

        if ((wmNews.getTitle() + content).length() == 0) {
            return flag;
        }

        // 下面的流程无法正常通过，因为本人并没有开启阿里云内容安全的付费服务
        /*try {
            Map map = greenTextScan.greeTextScan(wmNews.getTitle() + "|" + content);
            if(map != null) {
                if(map.get("suggestion").equals("block")) {
                    flag = false;
                    updateNewsStatus(wmNews, (short) 2, "当前文章中存在违规内容");
                }

                if(map.get("suggestion").equals("review")) {
                    flag = false;
                    updateNewsStatus(wmNews, (short) 3, "当前文章中存在不确定片段");
                }
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }*/

        // 人工审核
        updateNewsStatus(wmNews, (short) 3, "当前文章中存在不确定片段");

        return false;
    }

    /**
     * 更改文章状态
     *
     * @param wmNews
     * @param status
     * @param reason
     */
    private void updateNewsStatus(WmNews wmNews, Short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 1. 从自媒体文章的内容中提取纯文本和图片
     * 2. 提取文章中的封面图片
     *
     * @param wmNews
     * @return
     */
    private Map<String, Object> handlerTextAndImages(WmNews wmNews) {
        StringBuilder stringBuilder = new StringBuilder();
        List<String> images = new ArrayList<>();
        if (!StringUtils.isBlank(wmNews.getContent())) {
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if (map.get("type").equals("text")) {
                    stringBuilder.append(map.get("value"));
                } else if (map.get("type").equals("image")) {
                    images.add((String) map.get("value"));
                } else {
                    throw new RuntimeException("内容含有暂未支持的片段哦");
                }
            }
        }

        if (StringUtils.isNotBlank(wmNews.getImages())) {
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", stringBuilder.toString());
        result.put("images", images);

        return result;
    }
}
