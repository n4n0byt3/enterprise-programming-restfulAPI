package service;

import dao.BookDAO;
import model.Book;

import java.sql.SQLException;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Service layer for Book operations.
 * Sits between the servlet (HTTP layer) and the DAO (database layer).
 * Responsible for: business logic, input validation, and delegating to BookDAO.
 *
 * Date validation enforces DD/MM/YYYY or DD/MM/YY format for new entries.
 * Existing database entries are displayed as-is since the DB contains
 * mixed format legacy data that cannot be safely auto-cleansed.
 */
public class BookService {

    // Validates DD/MM/YYYY or DD/MM/YY
    private static final Pattern DATE_PATTERN =
            Pattern.compile("^(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[0-2])/(\\d{2}|\\d{4})$");

    private final BookDAO bookDAO;

    public BookService() {
        this.bookDAO = new BookDAO();
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Returns all books from the database.
     */
    public Collection<Book> getAllBooks() throws SQLException {
        return bookDAO.getAllBooks();
    }

    /**
     * Returns a single book by ID, or null if not found.
     */
    public Book getBookById(int id) throws SQLException {
        return bookDAO.getBookById(id);
    }

    /**
     * Returns all books matching the search term across title, author, genres, date.
     */
    public Collection<Book> searchBooks(String searchTerm) throws SQLException {
        return bookDAO.searchBooks(searchTerm);
    }

    // -------------------------------------------------------------------------
    // Write operations — all validate before hitting the DB
    // -------------------------------------------------------------------------

    /**
     * Inserts a new book after validating all fields.
     * @param book the Book to insert
     * @throws IllegalArgumentException if validation fails
     * @throws SQLException if database operation fails
     */
    public void insertBook(Book book) throws IllegalArgumentException, SQLException {
        validateBook(book);
        bookDAO.insertBook(book);
    }

    /**
     * Updates an existing book after validating all fields.
     * @param book the Book to update (must have a valid id)
     * @throws IllegalArgumentException if validation fails
     * @throws SQLException if database operation fails
     */
    public void updateBook(Book book) throws IllegalArgumentException, SQLException {
        if (book.getId() <= 0) {
            throw new IllegalArgumentException("Book id is required for update");
        }
        validateBook(book);
        bookDAO.updateBook(book);
    }

    /**
     * Deletes a book by ID.
     * @param id the primary key of the book to delete
     * @throws IllegalArgumentException if id is invalid
     * @throws SQLException if database operation fails
     */
    public void deleteBook(int id) throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid book id: " + id);
        }
        bookDAO.deleteBook(id);
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Validates a Book object before insert or update.
     * Rules:
     * - title and author are required
     * - date must match DD/MM/YYYY or DD/MM/YY if provided
     *
     * Note: existing DB records may contain MM/DD/YYYY legacy data.
     * This validation only applies to new input submitted via the API.
     *
     * @param book the Book to validate
     * @throws IllegalArgumentException if any field is invalid
     */
    private void validateBook(Book book) throws IllegalArgumentException {
        if (book == null) {
            throw new IllegalArgumentException("Book data is required");
        }
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Author is required");
        }
        if (book.getDate() != null && !book.getDate().trim().isEmpty()) {
            if (!DATE_PATTERN.matcher(book.getDate().trim()).matches()) {
                throw new IllegalArgumentException(
                    "Invalid date format. Expected DD/MM/YYYY or DD/MM/YY, got: " + book.getDate()
                );
            }
        }
    }
}