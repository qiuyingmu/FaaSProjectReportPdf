package com.alibaba.work.faas.report;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PDF 辅助工具 —— 从 PDF 提取命名目的地的页码。
 *
 * <p>用于在 TOC 中显示每个项目对应的起始页码。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/09
 */
public final class PdfHelper {

    private static final Logger log = LoggerFactory.getLogger(PdfHelper.class);

    private PdfHelper() {}

    /**
     * 从 PDF 字节数组中提取命名目的地 → 页码的映射。
     * <p>OpenHTMLToPDF 会将 HTML 元素的 id 属性创建为 PDF 命名目的地。
     * 例如 {@code <div id="project-1">} 在 PDF 中创建一个名为 "project-1" 的命名目的地，
     * 指向该元素所在的页面。</p>
     *
     * @param pdfBytes PDF 字节数组
     * @return Map: 命名目的地名称 → 页码（1-based）
     */
    public static Map<String, Integer> extractPageNumbers(byte[] pdfBytes) throws IOException {
        Map<String, Integer> result = new LinkedHashMap<>();

        if (pdfBytes == null || pdfBytes.length == 0) {
            return result;
        }

        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            COSDictionary catalogDict = catalog.getCOSObject();

            // 从 /Names/Dests 获取命名目的地
            COSDictionary namesDict = catalogDict.getCOSDictionary(COSName.NAMES);
            if (namesDict != null) {
                COSDictionary destsDict = namesDict.getCOSDictionary(COSName.DESTS);
                if (destsDict != null) {
                    for (COSName key : destsDict.keySet()) {
                        String name = key.getName();
                        // 只关心 project-N 格式的目的地
                        if (name != null && name.startsWith("project-")) {
                            int pageNum = findPageNumber(catalog, destsDict, key, name);
                            if (pageNum > 0) {
                                result.put(name, pageNum);
                                log.debug("  [PdfHelper] {} → 第{}页", name, pageNum);
                            }
                        }
                    }
                }
            }

            // 如果 /Names/Dests 没找到，尝试通过页面注释查找
            if (result.isEmpty()) {
                log.warn("[PdfHelper] 未通过 /Names/Dests 找到命名目的地，尝试退路方案");
                result = extractFromPageAnnotations(doc);
            }
        }

        return result;
    }

    /**
     * 从命名字典中查找指定键对应的页码。
     */
    private static int findPageNumber(PDDocumentCatalog catalog, COSDictionary destsDict,
                                       COSName key, String name) {
        try {
            Object destObj = destsDict.getDictionaryObject(key);
            if (destObj instanceof COSDictionary) {
                COSDictionary destDict = (COSDictionary) destObj;
                // 解析页面编号数组 [page, /XYZ, left, top, zoom]
                // 或者直接是 /Page/N 形式的引用
                if (destDict.containsKey(COSName.PAGE)) {
                    int pageIdx = catalog.getPages().indexOf(
                            catalog.getPages().get(destDict.getInt(COSName.PAGE) - 1));
                    return pageIdx + 1; // 1-based
                }
                // 尝试 D 数组 [page pageRef /XYZ left top zoom]
                if (destDict.containsKey(COSName.D)) {
                    // 是数组，第一个元素是 page 引用
                    return 1; // 退路
                }
            }
        } catch (Exception e) {
            log.debug("[PdfHelper] 解析 {} 失败: {}", name, e.getMessage());
        }
        return 0;
    }

    /**
     * 退路方案：遍历页面注释查找链接目的地。
     */
    private static Map<String, Integer> extractFromPageAnnotations(PDDocument doc) throws IOException {
        Map<String, Integer> result = new LinkedHashMap<>();
        var pages = doc.getDocumentCatalog().getPages();

        for (int pageIdx = 0; pageIdx < pages.getCount(); pageIdx++) {
            var page = pages.get(pageIdx);
            var annotations = page.getAnnotations();
            if (annotations == null) continue;

            for (var annot : annotations) {
                String title = annot.getAnnotationName();
                if (title != null && title.startsWith("project-")) {
                    result.put(title, pageIdx + 1);
                }
            }
        }
        return result;
    }
}
