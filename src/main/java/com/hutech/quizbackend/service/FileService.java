package com.hutech.quizbackend.service;

import org.apache.pdfbox.Loader; // Quan trọng: Dùng Loader thay cho PDDocument trực tiếp
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.stream.Collectors;

@Service
public class FileService {

    public String extractText(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) return "";

        // Chống Upload mã độc: Chỉ xử lý các định dạng được phép
        if (fileName.toLowerCase().endsWith(".pdf")) {
            // Sửa lỗi ở đây: Dùng Loader.loadPDF cho bản PDFBox 3.0+
            try (InputStream is = file.getInputStream();
                 PDDocument document = Loader.loadPDF(is.readAllBytes())) {
                return new PDFTextStripper().getText(document);
            }
        } else if (fileName.toLowerCase().endsWith(".docx")) {
            try (InputStream is = file.getInputStream();
                 XWPFDocument doc = new XWPFDocument(is)) {
                return doc.getParagraphs().stream()
                        .map(XWPFParagraph::getText)
                        .collect(Collectors.joining("\n"));
            }
        } else if (fileName.toLowerCase().endsWith(".txt")) {
            return new String(file.getBytes());
        }

        throw new IllegalArgumentException("Định dạng file không được hỗ trợ!");
    }
}