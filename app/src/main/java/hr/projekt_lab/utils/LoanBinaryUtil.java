package hr.projekt_lab.utils;

import hr.projekt_lab.entities.Author;
import hr.projekt_lab.entities.Book;
import hr.projekt_lab.entities.Genre;
import hr.projekt_lab.entities.Loan;
import hr.projekt_lab.entities.Magazine;
import hr.projekt_lab.entities.Member;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Pomoćna klasa za binarni backup i restore posudbi.
 *
 * Vlastiti binarni format (.bin):
 *   [int]  broj zapisa
 *   Za svaki zapis:
 *     [int]   loanId  (0 ako nije poznat)
 *     [UTF]   memberId
 *     [UTF]   memberFirstName
 *     [UTF]   memberLastName
 *     [long]  loanDate  (LocalDate.toEpochDay())
 *     [long]  returnDate (LocalDate.toEpochDay())
 *     [byte]  tip publikacije: 0 = knjiga, 1 = časopis
 *     ako knjiga:
 *       [UTF]  isbn
 *       [UTF]  bookTitle
 *     ako časopis:
 *       [UTF]  magazineTitle
 */
public final class LoanBinaryUtil {

    public static final File BACKUP_FILE = new File("data/loans-backup.bin");

    private static final byte TYPE_BOOK     = 0;
    private static final byte TYPE_MAGAZINE = 1;

    /**
     * ReentrantLock koji štiti pisanje u backup datoteku.
     *
     * Za razliku od {@code synchronized} bloka, ReentrantLock nudi:
     * <ul>
     *   <li>eksplicitno zaključavanje/otključavanje (vidljivo u kodu)</li>
     *   <li>mogućnost isprobavanja bez blokiranja ({@code tryLock()})</li>
     *   <li>interruptible zaključavanje i fair-mode opciju</li>
     * </ul>
     * Garantira da samo jedna dretva istovremeno piše u {@code loans-backup.bin},
     * sprječavajući oštećenje datoteke pri istovremenom pozivu backup-a.
     */
    private static final ReentrantLock BACKUP_LOCK = new ReentrantLock();

    private LoanBinaryUtil() {}

    public static void zapiši(List<Loan> loans) {
        File parent = BACKUP_FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        // ReentrantLock: eksplicitno zaključavanje prije pisanja u datoteku.
        // try-finally garantira otključavanje čak i ako dođe do iznimke.
        BACKUP_LOCK.lock();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (DataOutputStream dos = new DataOutputStream(bos)) {

                dos.writeInt(loans.size());

                for (Loan loan : loans) {
                    dos.writeInt(loan.getLoanId() != null ? loan.getLoanId() : 0);

                    dos.writeUTF(loan.getMember().getId() != null
                            ? loan.getMember().getId() : "");
                    dos.writeUTF(loan.getMember().getFirstName() != null
                            ? loan.getMember().getFirstName() : "");
                    dos.writeUTF(loan.getMember().getLastName() != null
                            ? loan.getMember().getLastName() : "");

                    dos.writeLong(loan.getLoanDate().toEpochDay());
                    dos.writeLong(loan.getReturnDate().toEpochDay());

                    if (loan.getBook().isPresent()) {
                        dos.writeByte(TYPE_BOOK);
                        dos.writeUTF(loan.getBook().get().getIsbn() != null
                                ? loan.getBook().get().getIsbn() : "");
                        dos.writeUTF(loan.getBook().get().getTitle() != null
                                ? loan.getBook().get().getTitle() : "");
                    } else {
                        dos.writeByte(TYPE_MAGAZINE);
                        dos.writeUTF(loan.getMagazine()
                                .map(Magazine::getTitle)
                                .orElse(""));
                    }
                }
            }

            byte[] encrypted = CryptoUtils.encrypt(bos.toByteArray());
            try (FileOutputStream fos = new FileOutputStream(BACKUP_FILE)) {
                fos.write(encrypted);
            }

        } catch (IOException e) {
            throw new RuntimeException("Greška pri zapisu binarnog backupa posudbi.", e);
        } finally {
            // ReentrantLock mora biti eksplicitno otključan u finally bloku
            BACKUP_LOCK.unlock();
        }
    }

    public static List<Loan> učitaj() {
        List<Loan> loans = new ArrayList<>();

        if (!BACKUP_FILE.exists()) {
            return loans;
        }

        try {
            byte[] encryptedBytes = Files.readAllBytes(BACKUP_FILE.toPath());
            byte[] plainBytes;
            try {
                plainBytes = CryptoUtils.decrypt(encryptedBytes);
            } catch (RuntimeException decryptEx) {
                // Stara nešifrirana datoteka — briše se kako bi sljedeći backup radio ispravno
                BACKUP_FILE.delete();
                return loans;
            }

            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(plainBytes));
            int count = dis.readInt();

            for (int i = 0; i < count; i++) {
                int loanId      = dis.readInt();
                String memberId = dis.readUTF();
                String firstName = dis.readUTF();
                String lastName  = dis.readUTF();
                LocalDate loanDate   = LocalDate.ofEpochDay(dis.readLong());
                LocalDate returnDate = LocalDate.ofEpochDay(dis.readLong());
                byte type = dis.readByte();

                Member member = new Member(firstName, lastName, loanDate);
                member.setId(memberId);

                Optional<Book> book = Optional.empty();
                Optional<Magazine> magazine = Optional.empty();

                if (type == TYPE_BOOK) {
                    String isbn      = dis.readUTF();
                    String bookTitle = dis.readUTF();
                    Author dummyAuthor = new Author("", "", Year.now(), "");
                    Book b = new Book.BookBuilder(bookTitle, dummyAuthor,
                            Year.now(), isbn, Genre.KLASICNI_ROMAN).build();
                    book = Optional.of(b);
                } else {
                    String magTitle = dis.readUTF();
                    Magazine m = new Magazine.MagazineBuilder(
                            magTitle, "", 0, Year.now(), "", "").build();
                    magazine = Optional.of(m);
                }

                Loan loan = new Loan(member, book, magazine, loanDate, returnDate);
                loan.setLoanId(loanId == 0 ? null : loanId);
                loans.add(loan);
            }

        } catch (IOException e) {
            throw new RuntimeException("Greška pri čitanju binarnog backupa posudbi.", e);
        }

        return loans;
    }
}
