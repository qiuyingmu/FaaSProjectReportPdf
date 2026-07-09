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
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 辅助工具 —— 从 PDF 提取项目起始页码。
 *
 * <p>优先通过嵌入的不可见文本标记（如 {@code __PROJECT_PAGE_1__}）精确定位；
 * 若标记不存在，则回退到命名目的地解析。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/09
 */
public final class PdfHelper {

    private static final Logger log = LoggerFactory.getLogger(PdfHelper.class);

    private static final Pattern MARKER_PATTERN = Pattern.compile("__PROJECT_PAGE_(\\d+)__");

    private PdfHelper() {}

    /**
     * 从 PDF 字节数组中提取项目起始页码。
     *
     * <p>返回值：project index（1-based） → 页码（1-based）。</p>
     */
    public static Map<Integer, Integer> extractPageNumbers(byte[] pdfBytes) throws IOException {
        Map<Integer, Integer> result = extractByMarkers(pdfBytes);

        if (!result.isEmpty()) {
            log.info("[PdfHelper] 通过文本标记提取到 {} 个项目页码", result.size());
            return result;
        }

        log.warn("[PdfHelper] 未找到项目页码文本标记，尝试命名目的地退路");
        result = extractFromNamedDestinations(pdfBytes);

        if (!result.isEmpty()) {
            log.info("[PdfHelper] 通过命名目的地提取到 {} 个项目页码", result.size());
        } else {
            log.warn("[PdfHelper] 未能从 PDF 提取到任何 project-N 页码");
        }

        return result;
    }

    /**
     * 通过嵌入的不可见文本标记提取页码。
     */
    private static Map<Integer, Integer> extractByMarkers(byte[] pdfBytes) throws IOException {
        Map<Integer, Integer> result = new LinkedHashMap<>();

        if (pdfBytes == null || pdfBytes.length == 0) {
            return result;
        }

        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            int pageCount = doc.getNumberOfPages();
            if (pageCount == 0) {
                return result;
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String text = stripper.getText(doc);

                Matcher m = MARKER_PATTERN.matcher(text);
                while (m.find()) {
                    int index = Integer.parseInt(m.group(1));
                    // 只记录首次出现的页码（即项目起始页）
                    result.putIfAbsent(index, pageNum);
                }
            }
        }

        return result;
    }

    /**
     * 从 PDF 命名目的地（/Names/Dests）提取页码。
     */
    private static Map<Integer, Integer> extractFromNamedDestinations(byte[] pdfBytes) throws IOException {
        Map<Integer, Integer> result = new LinkedHashMap<>();

        if (pdfBytes == null || pdfBytes.length == 0) {
            return result;
        }

        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            PDPageTree pages = catalog.getPages();
            COSDictionary catalogDict = catalog.getCOSObject();

            COSDictionary namesDict = catalogDict.getCOSDictionary(COSName.NAMES);
            if (namesDict != null) {
                COSDictionary destsDict = namesDict.getCOSDictionary(COSName.DESTS);
                if (destsDict != null) {
                    for (COSName key : destsDict.keySet()) {
                        String name = key.getName();
                        if (name != null && name.startsWith("project-")) {
                            int pageNum = resolveNamedDestinationPage(destsDict, key, pages);
                            if (pageNum > 0) {
                                int index = Integer.parseInt(name.substring(name.indexOf('-') + 1));
                                result.put(index, pageNum);
                                log.debug("  [PdfHelper] {} → 第{}页", name, pageNum);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private static int resolveNamedDestinationPage(COSDictionary destsDict, COSName key,
                                                    PDPageTree pages) {
        try {
            COSBase destValue = destsDict.getDictionaryObject(key);
            if (destValue instanceof COSObject) {
                destValue = ((COSObject) destValue).getObject();
            }

            COSArray destArray = null;

            if (destValue instanceof COSArray) {
                destArray = (COSArray) destValue;
            } else if (destValue instanceof COSDictionary) {
                COSDictionary destDict = (COSDictionary) destValue;
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
                        return idx + 1;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[PdfHelper] 解析 {} 失败: {}", key.getName(), e.getMessage());
        }
        return 0;
    }
}
