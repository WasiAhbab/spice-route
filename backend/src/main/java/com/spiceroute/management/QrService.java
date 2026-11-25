package com.spiceroute.management;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.spiceroute.security.AccessService;
import com.spiceroute.security.AuthService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QrService {

    private final DiningTableRepository tables;
    private final AccessService access;
    private final String publicUrl;

    public QrService(DiningTableRepository tables, AccessService access,
                     @Value("${app.public-url}") String publicUrl) {
        this.tables = tables;
        this.access = access;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    public byte[] table(AuthService.Actor actor, UUID tableId) throws Exception {
        access.table(actor, tableId);
        return png(tables.findById(tableId).orElseThrow().getToken());
    }

    public byte[] branch(AuthService.Actor actor, UUID branchId) throws Exception {
        access.branch(actor, branchId);
        List<DiningTable> activeTables = tables.findByBranch_IdAndActiveTrueOrderByLabelAsc(branchId);
        if (activeTables.isEmpty()) throw new IllegalArgumentException("No active tables");
        if (activeTables.size() > 250) throw new IllegalArgumentException("A QR export supports up to 250 tables");
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (DiningTable table : activeTables) {
                addPage(document, table);
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] png(UUID token) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        var matrix = new QRCodeWriter().encode(publicUrl + "/feedback/" + token, BarcodeFormat.QR_CODE, 350, 350);
        MatrixToImageWriter.writeToStream(matrix, "PNG", output);
        return output.toByteArray();
    }

    private void addPage(PDDocument document, DiningTable table) throws Exception {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.drawImage(PDImageXObject.createFromByteArray(document, png(table.getToken()), "QR"),
                156, 300, 300, 300);
            String label = table.getLabel().replaceAll("[^ -~]", "?");
            if (label.length() > 40) label = label.substring(0, 37) + "...";
            addText(content, "Table " + label, 20, 100, 650, Standard14Fonts.FontName.HELVETICA_BOLD);
            addText(content, "Scan to share your feedback", 14, 156, 270, Standard14Fonts.FontName.HELVETICA);
        }
    }

    private void addText(PDPageContentStream content, String text, int size, int x, int y,
                         Standard14Fonts.FontName font) throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(font), size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }
}
