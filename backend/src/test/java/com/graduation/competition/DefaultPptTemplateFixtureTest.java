package com.graduation.competition;

import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 生成并维护仓库内默认模板。已有模板时只做存在性检查。
 */
class DefaultPptTemplateFixtureTest {

    @Test
    void createsDefaultTemplate() throws Exception {
        Path target = Path.of("src/main/resources/templates/default.pptx");
        if (Files.exists(target)) {
            return;
        }
        Files.createDirectories(target.getParent());
        try (XMLSlideShow show = new XMLSlideShow()) {
            slide(show, "{{title}}", "{{subtitle}}", "证据驱动的学科竞赛项目助手");
            slide(show, "项目背景与需求", "{{background}}\n\n{{requirements}}", "明确问题、用户与作品边界");
            slide(show, "功能模块", "{{modules}}", "从最小可用闭环开始");
            slide(show, "技术路线与创新点", "{{techRoute}}\n\n创新点：{{innovation}}", "可替换、可测试、可演示");
            slide(show, "开发计划", "{{schedule}}", "以规则核验与演示验收收尾");
            try (var output = Files.newOutputStream(target)) {
                show.write(output);
            }
        }
    }

    private void slide(XMLSlideShow show, String title, String body, String note) {
        XSLFSlide slide = show.createSlide();
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(70, 45, 800, 85));
        titleBox.setText(title);
        titleBox.getTextParagraphs().get(0).setTextAlign(TextParagraph.TextAlign.LEFT);
        titleBox.getTextParagraphs().get(0).getTextRuns().get(0).setFontSize(30.0);

        XSLFTextBox bodyBox = slide.createTextBox();
        bodyBox.setAnchor(new Rectangle(70, 155, 820, 330));
        bodyBox.setText(body);
        bodyBox.getTextParagraphs().get(0).getTextRuns().get(0).setFontSize(18.0);

        XSLFTextBox noteBox = slide.createTextBox();
        noteBox.setAnchor(new Rectangle(70, 510, 820, 40));
        noteBox.setText(note);
        noteBox.getTextParagraphs().get(0).getTextRuns().get(0).setFontSize(11.0);
    }
}

