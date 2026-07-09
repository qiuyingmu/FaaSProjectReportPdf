package com.alibaba.work.faas.report;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
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
            PDPageTree pages = catalog.getPages();
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
                            int pageNum = resolveNamedDestinationPage(destsDict, key, pages);
                            if (pageNum > 0) {
                                result.put(name, pageNum);
                                log.debug("  [PdfHelper] {} → 第{}页", name, pageNum);
                            }
                        }
                    }
                }
            }

            // 如果 /Names/Dests 没找到，尝试通过页面链接注释查找
            if (result.isEmpty()) {
                log.warn("[PdfHelper] 未通过 /Names/Dests 找到命名目的地，尝试链接注释退路方案");
                result = extractFromLinkAnnotations(doc);
            }
        }

        if (result.isEmpty()) {
            log.warn("[PdfHelper] 未能从 PDF 提取到任何 project-N 页码");
        } else {
            log.info("[PdfHelper] 成功提取 {} 个项目页码", result.size());
        }

        return result;
    }

    /**
     * 解析命名字典中某个命名目的地对应的页码。
     *
     * <p>PDF 中命名目的地的值可能是：</p>
     * <ul>
     *   <li>数组：{@code [pageRef /XYZ left top zoom]}</li>
     *   <li>字典：{@code /D -> [pageRef /XYZ left top zoom]} 或 {@code /SD -> ...}</li>
     * </ul>
     */
    private static int resolveNamedDestinationPage(COSDictionary destsDict, COSName key,
                                                    PDPageTree pages) {
        try {
            COSBase destValue = destsDict.getDictionaryObject(key);
            if (destValue instanceof COSObject) {
                destValue = ((COSObject) destValue).getObject();
            }

            COSArray destArray = null;

            if (destValue instanceof COSArray) {
                // 直接是数组
                destArray = (COSArray) destValue;
            } else if (destValue instanceof COSDictionary) {
                COSDictionary destDict = (COSDictionary) destValue;
                // 优先 /D，其次 /SD
                COSBase d = destDict.getDictionaryObject(COSName.D);
                if (d instanceof COSObject) {
                    d = ((COSObject) d).getObject();
                }
                if (d instanceof COSArray) {
                    destArray = (COSArray) d;
                } else {
                    COSBase sd = destDict.getDictionaryObject(COSName.getPDFName("SD"));
                    if (sd instanceof COSObject) {
                        sd = ((COSObject) sd).getObject();
                    }
                    if (sd instanceof COSArray) {
                        destArray = (COSArray) sd;
                    }
                }
            }

            if (destArray != null && destArray.size() > 0) {
                COSBase pageRef = destArray.get(0);
                if (pageRef instanceof COSObject) {
                    pageRef = ((COSObject) pageRef).getObject();
                }
                if (pageRef instanceof COSDictionary) {
                    PDPage page = new PDPage((COSDictionary) pageRef);
                    int idx = pages.indexOf(page);
                    if (idx >= 0) {
                        return idx + 1; // 1-based
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[PdfHelper] 解析 {} 失败: {}", key.getName(), e.getMessage());
        }
        return 0;
    }

    /**
     * 退路方案：遍历页面链接注释，查找指向 project-N 命名目的地的注释，
     * 从而推断各项目所在页码。
     */
    private static Map<String, Integer> extractFromLinkAnnotations(PDDocument doc) throws IOException {
        Map<String, Integer> result = new LinkedHashMap<>();
        PDPageTree pages = doc.getDocumentCatalog().getPages();

        for (int pageIdx = 0; pageIdx < pages.getCount(); pageIdx++) {
            PDPage page = pages.get(pageIdx);
            var annotations = page.getAnnotations();
            if (annotations == null) continue;

            for (var annot : annotations) {
                if (!(annot instanceof PDAnnotationLink)) {
                    continue;
                }
                PDAnnotationLink link = (PDAnnotationLink) annot;
                PDDestination dest = link.getDestination();
                if (dest instanceof PDPageDestination) {
                    PDPageDestination pageDest = (PDPageDestination) dest;
                    PDPage destPage = pageDest.getPage();
                    if (destPage == null) continue;
                    // 链接注释本身就在 destPage 上？不，这里需要从命名目的地反查
                }

                // 尝试从 Action /GoTo /D 获取命名目的地
                var action = link.getAction();
                if (action instanceof org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo) {
                    var gotoAction = (org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo) action;
                    var d = gotoAction.getDestination();
                    if (d instanceof PDPageDestination) {
                        String name = resolveLinkName(link);
                        if (name != null && name.startsWith("project-")) {
                            result.putIfAbsent(name, pageIdx + 1);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static String resolveLinkName(PDAnnotationLink link) {
        // OpenHTMLToPDF 生成的链接注释通常没有名称；
        // 此退路主要作为结构保留，实际有效性依赖 PDF 生成器行为。
        return link.getAnnotationName();
    }
}
