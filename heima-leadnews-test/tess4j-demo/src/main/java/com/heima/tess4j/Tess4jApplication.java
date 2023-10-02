package com.heima.tess4j;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;

public class Tess4jApplication {
    /**
     * 识别图片中文字
     * @param args
     */
    public static void main(String[] args) throws TesseractException {

        // 创建实例
        ITesseract tesseract = new Tesseract();

        // 设置字体库路径
        tesseract.setDatapath("E:\\code\\Java_workpace\\hmtt\\tess4j_library");

        // 设置语言 简体中文
        tesseract.setLanguage("chi_sim");

        File file = new File("F:\\OCR_TEST.jpg");

        // 识别图片
        String s = tesseract.doOCR(file);

        System.out.println("识别结果为: " + s.replaceAll("\\r|\\n", "-"));
    }
}