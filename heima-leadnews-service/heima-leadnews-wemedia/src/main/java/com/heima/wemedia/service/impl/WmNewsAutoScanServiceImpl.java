package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.file.service.FileStorageService;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void autoScanWmNews(Integer id) {
        // 1. 查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            throw new RuntimeException("WmNewsAutoScanServiceImpl - 文章不存在");
        }

        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {

            // 从内容中提取纯文本和图片
            Map<String, Object> textAndImages = handlerTextAndImages(wmNews);

            // 2. 审核文本内容
            boolean isTextScan = handlerTextScan((String) textAndImages.get("content"), wmNews);
            if (!isTextScan) {
                return;
            }

            // 3. 审核图片内容
            boolean isImageScan = handlerImageScan((List<String>) textAndImages.get("images"), wmNews);
            if (!isImageScan) {
                return;
            }

            // 4. 审核成功，保存app端的相关文章数据
        }
    }

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GreenImageScan greenImageScan;

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
        /*images = images.stream().distinct().collect(Collectors.toList());

        // 下面的流程无法正常通过，因为本人并没有开启阿里云内容安全的付费服务
        List<byte[]> imageList = new ArrayList<>();
        for(String image : images) {
            byte[] bytes = fileStorageService.downLoadFile(image);
            imageList.add(bytes);
        }

        // 审核图片
        try {
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

        // 人工审核
        updateNewsStatus(wmNews, (short) 3, "当前文章中存在不确定片段");
        return false;
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
        if (StringUtils.isBlank(wmNews.getContent())) {
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
