package com.library.library_management;

import com.library.library_management.model.Book;
import com.library.library_management.model.User;
import com.library.library_management.model.Role;
import com.library.library_management.repository.BookRepository;
import com.library.library_management.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the database on first startup.
 * Skips silently if data already exists — safe to keep in production.
 *
 * Default credentials after seeding:
 *   ADMIN → email: Ahmed@google.com  password: admin123
 *   USER  → email: Ahmed@User.com    password: user123
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(BookRepository bookRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedBooks();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  USERS
    // ─────────────────────────────────────────────────────────────────────────

    private void seedUsers() {
        if (userRepository.count() > 0) return;

        User admin = new User();
        admin.setFirstName("Ahmed");
        admin.setLastName("Admin");
        admin.setEmail("Ahmed@google.com");
        admin.setPassword(passwordEncoder.encode("Password123"));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        User member = new User();
        member.setFirstName("Ahmed");
        member.setLastName("User");
        member.setEmail("Ahmed@User.com");
        member.setPassword(passwordEncoder.encode("Password123"));
        member.setRole(Role.USER);
        member.setActive(true);

        userRepository.saveAll(List.of(admin, member));
        System.out.println("✅ Seeded 2 users");
        System.out.println("   ADMIN → Ahmed@google.com / admin123");
        System.out.println("   USER  → Ahmed@User.com   / user123");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BOOKS
    // ─────────────────────────────────────────────────────────────────────────

    private void seedBooks() {
        if (bookRepository.count() > 0) return;

        List<Book> books = List.of(

            // ── Moroccan Literature ───────────────────────────────────────────
            book("La Boite a merveilles",
                 "Ahmed Sefrioui", "9782070360647", 1954, 5, "Moroccan Literature",
                 "https://m.media-amazon.com/images/I/51wfbHG7f1L._AC_UF350,350_QL50_.jpg"),

            book("Le Pain nu",
                 "Mohamed Choukri", "9782020249232", 1973, 4, "Moroccan Literature",
                 "https://m.media-amazon.com/images/S/compressed.photo.goodreads.com/books/1582393525i/51567095.jpg"),

            book("Le Passe simple",
                 "Driss Chraibi", "9782070368839", 1954, 4, "Moroccan Literature",
                 "https://m.media-amazon.com/images/S/compressed.photo.goodreads.com/books/1359980627i/256373.jpg"),

            book("La Civilisation, ma mere !",
                 "Driss Chraibi", "9782070368846", 1972, 3, "Moroccan Literature",
                 "https://m.media-amazon.com/images/S/compressed.photo.goodreads.com/books/1352477336i/1483399.jpg"),

            book("L'Enfant de sable",
                 "Tahar Ben Jelloun", "9782070374335", 1985, 5, "Moroccan Literature",
                 "https://m.media-amazon.com/images/I/71ET6+aC4KL._AC_UF1000,1000_QL80_.jpg"),

            book("La Nuit sacree",
                 "Tahar Ben Jelloun", "9782070374342", 1987, 4, "Moroccan Literature",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSQ92VvWuBzZYuIckJBtbV8ONubqTK0viotRQ&s"),

            book("Cette aveuglante absence de lumiere",
                 "Tahar Ben Jelloun", "9782020419437", 2001, 4, "Moroccan Literature",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRI8l6oW_YkdNY8KMCi0ex4qYiFqXO9qAGI5w&s"),

            book("La Memoire tatouee",
                 "Abdelkebir Khatibi", "9782707100290", 1971, 3, "Moroccan Literature",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTgzqZweUyiYXrqsCM3_Osb4Lt2eWxaibICDg&s"),

            book("Harrouda",
                 "Tahar Ben Jelloun", "9782707100306", 1973, 3, "Moroccan Literature",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQeL-krDLCK62QuJooSuiBqSAzA6zaZQacxpQ&s"),

            book("Le Livre du sang",
                 "Abdelkebir Khatibi", "9782707100313", 1979, 3, "Moroccan Literature",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRDOMd-dxVUc1r4nSiyceO1cxygSmZdzLP2Yw&s"),

            // ── Self-Help ─────────────────────────────────────────────────────
            book("The 7 Habits of Highly Effective People",
                 "Stephen R. Covey", "9780743269", 1989, 3, "Self-Help",
                 "https://m.media-amazon.com/images/S/compressed.photo.goodreads.com/books/1421842784i/36072.jpg"),

            // ── Adventure ─────────────────────────────────────────────────────
            book("Treasure Island",
                 "Robert Louis Stevenson", "9780141321004", 1883, 5, "Adventure",
                 "https://m.media-amazon.com/images/I/91YuctKKlJL._AC_UF1000,1000_QL80_.jpg"),

            // ── French Classic ────────────────────────────────────────────────
            book("Le Dernier Jour d'un condamne",
                 "Victor Hugo", "9782070409228", 1829, 6, "French Classic",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS6rzNilVcEzwGdpVovh4giivaHt6mIAdpUBA&s"),

            book("Les Miserables",
                 "Victor Hugo", "9782070409235", 1862, 6, "French Classic",
                 "https://d28hgpri8am2if.cloudfront.net/book_images/cvr9781416500261_9781416500261_hr.jpg"),

            book("Le Comte de Monte-Cristo",
                 "Alexandre Dumas", "9782070409242", 1844, 5, "French Classic",
                 "https://images.epagine.fr/374/9782070405374_1_75.jpg"),

            book("Le Petit Prince",
                 "Antoine de Saint-Exupery", "9782070408504", 1943, 7, "French Classic",
                 "https://m.media-amazon.com/images/I/61NGp-UxolL._AC_UF1000,1000_QL80_.jpg"),

            book("L'Etranger",
                 "Albert Camus", "9782070360024", 1942, 6, "French Classic",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS6-Lp-FoNbYKEcrCw3jHZuC7xinw3IxUXVHQ&s"),

            book("Candide",
                 "Voltaire", "9782070360031", 1759, 5, "French Classic",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTWxMs0jp1qvurB38Vlg9JS0WgfG2w2x__M2A&s"),

            book("Madame Bovary",
                 "Gustave Flaubert", "9782070360055", 1857, 4, "French Classic",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRsRMUlWTt6krsSnVr0XvS4Mh0fkvzyv3jIXw&s"),

            book("Voyage au bout de la nuit",
                 "Louis-Ferdinand Celine", "9782070360062", 1932, 4, "French Classic",
                 "https://images.epagine.fr/894/9782072446894_1_75.jpg"),

            // ── World Fiction ─────────────────────────────────────────────────
            book("Don Quichotte",
                 "Miguel de Cervantes", "9782070360079", 1605, 4, "World Fiction",
                 "https://images.epagine.fr/898/9782344053898_1_75.jpg"),

            book("1984",
                 "George Orwell", "9780451524935", 1949, 6, "World Fiction",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRcUyUxOyVYke28l-GAMTx9bRqJTkrCE-fzYQ&s"),

            book("Brave New World",
                 "Aldous Huxley", "9780060850524", 1932, 5, "World Fiction",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQEq0k1I0QmDlDAvR28gpZ5Jj7_1IwQEoZ_Vg&s"),

            book("The Alchemist",
                 "Paulo Coelho", "9780062315007", 1988, 7, "World Fiction",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS3woEE7GEkETpuxo3BcB7VTB5aBlKVSbMy1g&s"),

            book("Crime and Punishment",
                 "Fyodor Dostoevsky", "B0FSKZ4FNW", 1866, 99, "Fiction",
                 "https://m.media-amazon.com/images/S/compressed.photo.goodreads.com/books/1494307172i/35101197.jpg"),

            book("Crime and Punishment",
                 "Fyodor Dostoevsky", "9780140449136", 1866, 5, "World Fiction",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRyrreC33JdCNt65c5PH-QN-wefrh76IpIPPw&s"),

            book("Anna Karenina",
                 "Leo Tolstoy", "9780140449174", 1877, 4, "World Fiction",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRkux0e9B4krYqzZCidgmdKsxFezd1-9fMeZA&s"),

            book("The Metamorphosis",
                 "Franz Kafka", "9780553213690", 1915, 3, "World Fiction",
                 "https://m.media-amazon.com/images/I/51Lgzw5BD8L.jpg"),

            book("Moby Dick",
                 "Herman Melville", "9780142437247", 1851, 3, "World Fiction",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSRv_a5drcBj6Dqd5qdB17ByNunHIm6woW2pg&s"),

            book("Pride and Prejudice",
                 "Jane Austen", "9780141439518", 1813, 5, "World Fiction",
                 "https://www.readerswarehouse.co.za/cdn/shop/files/9781648337093_1.png?v=1752706729"),

            book("Great Expectations",
                 "Charles Dickens", "9780141439563", 1861, 4, "World Fiction",
                 "https://m.media-amazon.com/images/I/715lBsaI4sL._AC_UF1000,1000_QL80_.jpg"),

            // ── Fantasy ───────────────────────────────────────────────────────
            book("Harry Potter and the Philosopher's Stone",
                 "J.K. Rowling", "9780747532743", 1997, 8, "Fantasy",
                 "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS92GCoWcX2W-O3kObdzk5D3Qst4dvXTP6UeA&s"),

            book("The Lord of the Rings",
                 "J.R.R. Tolkien", "9780618640157", 1954, 6, "Fantasy",
                 "https://m.media-amazon.com/images/S/compressed.photo.goodreads.com/books/1566425108i/33.jpg")
        );

        bookRepository.saveAll(books);
        System.out.println("✅ Seeded " + books.size() + " books");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private Book book(String title, String author, String isbn,
                      int pubYear, int copies, String genre, String coverImageUrl) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setIsbn(isbn);
        b.setPubYear(pubYear);
        b.setCopiesAvailable(copies);
        b.setGenre(genre);
        b.setCoverImageUrl(coverImageUrl);
        return b;
    }
}
