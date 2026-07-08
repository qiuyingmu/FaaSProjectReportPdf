package com.alibaba.work.faas.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 日志查看 API（受 Spring Security 保护）。
 *
 * <p>提供后台管理页面的日志读取接口，支持实时查看最近 N 行日志。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@RestController
@RequestMapping("/api/admin")
public class LogController {

    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    @Value("${report.log.path:logs/report.log}")
    private String logFilePath;

    /**
     * 获取最近 N 行日志。
     *
     * @param lines 返回行数（默认 200）
     * @return 日志行列表
     */
    @GetMapping("/logs")
    public Map<String, Object> getLogs(@RequestParam(defaultValue = "200") int lines) {
        Map<String, Object> result = new LinkedHashMap<>();

        File logFile = new File(logFilePath);
        if (!logFile.exists() || !logFile.isFile()) {
            result.put("success", false);
            result.put("message", "日志文件不存在: " + logFile.getAbsolutePath());
            result.put("lines", Collections.emptyList());
            return result;
        }

        try {
            List<String> tail = tailFile(logFile, Math.min(lines, 5000));
            result.put("success", true);
            result.put("file", logFile.getAbsolutePath());
            result.put("size", logFile.length());
            result.put("count", tail.size());
            result.put("lines", tail);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "读取日志失败: " + e.getMessage());
            result.put("lines", Collections.emptyList());
        }

        return result;
    }

    /**
     * 读取文件末尾 N 行（高效实现，避免全量加载，正确解码 UTF-8）。
     */
    private List<String> tailFile(File file, int maxLines) throws IOException {
        List<String> lines = new ArrayList<>(maxLines);
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        try {
            long length = raf.length();
            if (length == 0) return lines;

            // 从文件末尾向前逐字节读取，把每一行的字节按反序收集，
            // 遇到换行后再反转并解码为 UTF-8 字符串。
            long pos = length - 1;
            int count = 0;
            List<Byte> byteBuf = new ArrayList<>();

            for (; pos >= 0; pos--) {
                raf.seek(pos);
                byte b = raf.readByte();

                if (b == '\n') {
                    if (!byteBuf.isEmpty()) {
                        // 反转字节并解码（正确处理多字节 UTF-8）
                        lines.add(0, decodeReversedBytes(byteBuf));
                        byteBuf.clear();
                        count++;
                        if (count >= maxLines) break;
                    }
                } else if (b != '\r') {
                    byteBuf.add(b);
                }
            }

            // 处理文件开头可能没有换行的情况
            if (pos < 0 && !byteBuf.isEmpty() && count < maxLines) {
                lines.add(0, decodeReversedBytes(byteBuf));
            }

            return lines;
        } finally {
            raf.close();
        }
    }

    /**
     * 把反序收集的字节列表反转后按 UTF-8 解码为字符串。
     */
    private String decodeReversedBytes(List<Byte> reversedBytes) {
        int len = reversedBytes.size();
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = reversedBytes.get(len - 1 - i);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
