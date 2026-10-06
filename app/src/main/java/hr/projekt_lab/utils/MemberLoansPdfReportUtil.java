package hr.projekt_lab.utils;

import hr.projekt_lab.entities.Loan;
import hr.projekt_lab.entities.Member;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.text.Normalizer;

public final class MemberLoansPdfReportUtil {

    private static final float MARGIN = 50f;
    private static final float TITLE_FONT_SIZE = 16f;
    private static final float BODY_FONT_SIZE = 11f;
    private static final float LINE_HEIGHT = 16f;

    private MemberLoansPdfReportUtil() {
    }

    public static void generateReport(Member member, List<Loan> memberLoans, Path outputPath) throws IOException {
        LocalizationManager loc = LocalizationManager.getInstance();

        try (PDDocument document = new PDDocument()) {
            PageState pageState = newPage(document);

            pageState = writeLine(document, pageState, loc.getString("member.report.pdf_title"), TITLE_FONT_SIZE, true);
            pageState = writeLine(document, pageState, "", BODY_FONT_SIZE, false);

            String fullName = member.getFirstName() + " " + member.getLastName();
            pageState = writeLine(document, pageState,
                    loc.getString("member.report.label.member") + ": " + fullName,
                    BODY_FONT_SIZE, false);
            pageState = writeLine(document, pageState,
                    loc.getString("member.report.label.id") + ": " + member.getId(),
                    BODY_FONT_SIZE, false);
            pageState = writeLine(document, pageState,
                    loc.getString("member.report.label.join_date") + ": " + member.getMemberDate(),
                    BODY_FONT_SIZE, false);
            pageState = writeLine(document, pageState,
                    loc.getString("member.report.label.total_loans") + ": " + memberLoans.size(),
                    BODY_FONT_SIZE, false);
            pageState = writeLine(document, pageState, "", BODY_FONT_SIZE, false);

            pageState = writeLine(document, pageState,
                    loc.getString("member.report.detail_header"),
                    BODY_FONT_SIZE, true);

            if (memberLoans.isEmpty()) {
                pageState = writeLine(document, pageState,
                        loc.getString("member.report.no_loans"),
                        BODY_FONT_SIZE, false);
            } else {
                int counter = 1;
                for (Loan loan : memberLoans) {
                    String itemTitle = loan.getBook()
                            .map(book -> book.getTitle())
                            .or(() -> loan.getMagazine().map(magazine -> magazine.getTitle()))
                            .orElse("-");

                    String line = String.format(
                            "%d. %s: %s | %s: %s | %s: %s",
                            counter,
                            loc.getString("member.report.item"), itemTitle,
                            loc.getString("member.report.loan_date"), loan.getLoanDate(),
                            loc.getString("member.report.return_date"), loan.getReturnDate()
                    );

                    pageState = writeLine(document, pageState, line, BODY_FONT_SIZE, false);
                    counter++;
                }
            }

            pageState = writeLine(document, pageState, "", BODY_FONT_SIZE, false);
            String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            pageState = writeLine(document, pageState,
                    loc.getString("member.report.generated_at") + ": " + generatedAt,
                    BODY_FONT_SIZE, false);

            pageState.contentStream.close();

            document.save(outputPath.toFile());
        }
    }

    private static PageState newPage(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        float startY = page.getMediaBox().getHeight() - MARGIN;
        return new PageState(contentStream, startY);
    }

    private static PageState writeLine(PDDocument document, PageState state, String text,
                                       float fontSize, boolean bold) throws IOException {
        if (state.y < MARGIN) {
            state.contentStream.close();
            state = newPage(document);
        }

        state.contentStream.beginText();
        state.contentStream.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, fontSize);
        state.contentStream.newLineAtOffset(MARGIN, state.y);
        state.contentStream.showText(toPdfSafeText(text));
        state.contentStream.endText();

        state.y -= LINE_HEIGHT;
        return state;
    }

    private static String toPdfSafeText(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static final class PageState {
        private final PDPageContentStream contentStream;
        private float y;

        private PageState(PDPageContentStream contentStream, float y) {
            this.contentStream = contentStream;
            this.y = y;
        }
    }
}
