package com.graduation.competition.gen.ppt;

import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.plan.dto.PlanDTO;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Apache POI XSLF 模板填充器。只修改文本框内容，保持 PowerPoint 可编辑。
 */
@Component
public class PptGenerator {

    public byte[] generate(PlanDTO plan, String templateId) {
        if (templateId != null && !templateId.isBlank() && !"default".equals(templateId)) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "PPT 模板不存在: " + templateId);
        }
        try (InputStream input = getClass().getResourceAsStream("/templates/default.pptx")) {
            if (input == null) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "内置 PPT 模板缺失");
            }
            try (XMLSlideShow show = new XMLSlideShow(input);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                Map<String, String> values = values(plan);
                for (XSLFSlide slide : show.getSlides()) {
                    for (XSLFShape shape : slide.getShapes()) {
                        replaceShape(shape, values);
                    }
                }
                show.write(output);
                return output.toByteArray();
            }
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("PPT 生成失败", exception);
        }
    }

    private Map<String, String> values(PlanDTO plan) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("{{title}}", value(plan.title()));
        values.put("{{background}}", value(plan.background()));
        values.put("{{requirements}}", value(plan.requirements()));
        values.put("{{modules}}", modules(plan.modules()));
        values.put("{{techRoute}}", value(plan.techRoute()));
        values.put("{{innovation}}", value(plan.innovation()));
        values.put("{{schedule}}", value(plan.schedule()));
        values.put("{{subtitle}}", "学科竞赛项目方案 · 可编辑演示文稿");
        return values;
    }

    private void replaceShape(XSLFShape shape, Map<String, String> values) {
        if (shape instanceof XSLFTextShape textShape) {
            String text = textShape.getText();
            String replaced = replace(text, values);
            if (!text.equals(replaced)) {
                textShape.clearText();
                String[] lines = replaced.split("\\n", -1);
                textShape.setText(lines.length == 0 ? "" : lines[0]);
                for (int index = 1; index < lines.length; index++) {
                    textShape.addNewTextParagraph().addNewTextRun().setText(lines[index]);
                }
            }
        } else if (shape instanceof XSLFGroupShape group) {
            group.getShapes().forEach(child -> replaceShape(child, values));
        } else if (shape instanceof XSLFTable table) {
            for (org.apache.poi.xslf.usermodel.XSLFTableRow row : table.getRows()) {
                for (XSLFTableCell cell : row.getCells()) {
                    String replaced = replace(cell.getText(), values);
                    if (!replaced.equals(cell.getText())) {
                        cell.clearText();
                        cell.setText(replaced);
                    }
                }
            }
        }
    }

    private String replace(String text, Map<String, String> values) {
        String result = text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String modules(List<PlanDTO.ModuleDTO> modules) {
        if (modules == null || modules.isEmpty()) {
            return "• 暂无模块清单";
        }
        return modules.stream()
                .map(module -> "• " + value(module.name()) + "：" + value(module.description()))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}

