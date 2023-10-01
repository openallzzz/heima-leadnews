package com.heima.wemedia.test;

import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.file.service.FileStorageService;
import com.heima.wemedia.WemediaApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class AliyunTest {

    /**
     * 服务开通链接：75 元 / 10 万次
     * https://common-buy.aliyun.com/?spm=0.b82045537.0.0.5442CZu1CZu12i&commodityCode=lvwang_cipbag_dp_cn
     */

    @Autowired
    private GreenTextScan greenTextScan;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GreenImageScan greenImageScan;

    /**
     * 测试文本内容接口，并未使用其服务，因为要钱呀...
     */
    @Test
    public void testScanText() throws Exception {
        Map map = greenTextScan.greeTextScan("我是一个好人");
        System.out.println(map);
    }

    /**
     * 测试图片审核接口，并未使用其服务，因为要钱呀...
     */
    @Test
    public void testScanImage() throws Exception {
        byte[] bytes = fileStorageService
                .downLoadFile(
                        "http://192.168.200.130:9000/leadnews/2023/09/26/3fa7ac4ad1314eb2a65f7b2cde669eb8.png"
                );
        List<byte[]> list = new ArrayList<>();
        list.add(bytes);

        /**
         * 这段代码中的 uploadBytes->getCredentials->getCredentialsFromServer->client.doAction(uploadCredentialsRequest);
         * 调用链，最终提示图片上传服务未开启，无法再进行该流程，最终抛出异常
         */

        Map map = greenImageScan.imageScan(list);
        System.out.println(map);
    }

}
