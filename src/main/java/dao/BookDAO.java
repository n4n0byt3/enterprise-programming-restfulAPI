package dao;

import model.Book;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Data Access Object (DAO) for the books table.
 * All SQL uses PreparedStatements to prevent SQL injection.
 * Implements full CRUD: getAllBooks, getBookById, searchBooks,
 * insertBook, updateBook, deleteBook.
 */
public class BookDAO {

    /**
     * Retrieves all books from the database.
     * @return Collection of all Book objects
     */
    public Collection<Book> getAllBooks() throws SQLException {
        Collection<Book> books = new ArrayList<Book>();
        String sql = "SELECT * FROM books";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                books.add(mapRow(rs));
            }
        }
        return books;
    }

    /**
     * Retrieves a single book by its ID.
     * @param id the book's primary key
     * @return Book object, or null if not found
     */
    public Book getBookById(int id) throws SQLException {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Searches books by title, author, genres, or date.
     * @param searchStr the search term
     * @return Collection of matching Book objects
     */
    public Collection<Book> searchBooks(String searchStr) throws SQLException {
        Collection<Book> books = new ArrayList<Book>();
        String sql = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR genres LIKE ? OR date LIKE ?";
        String term = "%" + searchStr + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, term);
            ps.setString(2, term);
            ps.setString(3, term);
            ps.setString(4, term);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(mapRow(rs));
                }
            }
        }
        return books;
    }

    /**
     * Inserts a new book into the database.
     * @param b the Book object to insert
     */
    public void insertBook(Book b) throws SQLException {
        String sql = "INSERT INTO books (title, author, date, genres, characters, synopsis) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getTitle());
            ps.setString(2, b.getAuthor());
            ps.setString(3, b.getDate());
            ps.setString(4, b.getGenres());
            ps.setString(5, b.getCharacters());
            ps.setString(6, b.getSynopsis());
            ps.executeUpdate();
        }
    }

    /**
     * Updates an existing book record by ID.
     * @param b the Book object with updated fields (must have valid id)
     */
    public void updateBook(Book b) throws SQLException {
        String sql = "UPDATE books SET title = ?, author = ?, date = ?, genres = ?, characters = ?, synopsis = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getTitle());
            ps.setString(2, b.getAuthor());
            ps.setString(3, b.getDate());
            ps.setString(4, b.getGenres());
            ps.setString(5, b.getCharacters());
            ps.setString(6, b.getSynopsis());
            ps.setInt(7, b.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Deletes a book from the database by ID.
     * @param id the primary key of the book to delete
     */
    public void deleteBook(int id) throws SQLException {
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Maps a ResultSet row to a Book object.
     * @param rs the current ResultSet row
     * @return populated Book object
     */
    private Book mapRow(ResultSet rs) throws SQLException {
        return new Book(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("date"),
            rs.getString("genres"),
            rs.getString("characters"),
            rs.getString("synopsis")
        );
    }
}