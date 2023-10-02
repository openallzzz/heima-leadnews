package com.heima.article.service;

import com.heima.model.article.pojos.ApArticle;

public interface ArticleFreemarkerService {

    /**
     * 生成静态文件到minio中
     * @param article
     * @param content
     */
    public void buildArticlePageToMinIO(ApArticle article, String content);

}
