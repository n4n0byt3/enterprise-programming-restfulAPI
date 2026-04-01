package web.api;

import dao.BookDAO;
import model.Book;
import model.BookList;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Collection;

/**
 * RESTful API Servlet for the Book database.
 * Mapped to /Bookapi — handles GET, POST, PUT, DELETE.
 *
 * Format selection: client sends ?format=json|xml|text (default: json)
 * Or via Accept header: application/json | application/xml | text/plain
 *
 * GET    /Bookapi                  -> all books
 * GET    /Bookapi?id=X             -> single book by id
 * GET    /Bookapi?search=term      -> search books
 * POST   /Bookapi                  -> insert new book (body: book data)
 * PUT    /Bookapi                  -> update book (body: book data with id)
 * DELETE /Bookapi?id=X             -> delete book by id
 */
public class BookApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private BookDAO bookDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        bookDAO = new BookDAO();
        gson = new Gson();
    }

    // -------------------------------------------------------------------------
    // GET — retrieve all, by id, or by search term
    // -------------------------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String format = resolveFormat(request);
        String idParam = request.getParameter("id");
        String searchParam = request.getParameter("search");

        try {
            if (idParam != null) {
                // GET single book by id
                int id = Integer.parseInt(idParam);
                Book book = bookDAO.getBookById(id);
                if (book == null) {
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Book not found", format);
                    return;
                }
                sendBookResponse(response, book, format);

            } else if (searchParam != null) {
                // GET books matching search term
                Collection<Book> books = bookDAO.searchBooks(searchParam);
                sendBooksResponse(response, books, format);

            } else {
                // GET all books
                Collection<Book> books = bookDAO.getAllBooks();
                sendBooksResponse(response, books, format);
            }

        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid id parameter", format);
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage(), format);
        }
    }

    // -------------------------------------------------------------------------
    // POST — insert a new book
    // -------------------------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String format = resolveFormat(request);
        String body = readBody(request);

        try {
            Book book = parseBook(body, format);
            if (!isValidBook(book)) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing required fields: title and author are required", format);
                return;
            }
            bookDAO.insertBook(book);
            response.setStatus(HttpServletResponse.SC_CREATED);
            sendPlainMessage(response, "Book inserted successfully", format);

        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Failed to parse request body: " + e.getMessage(), format);
        }
    }

    // -------------------------------------------------------------------------
    // PUT — update an existing book
    // -------------------------------------------------------------------------
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String format = resolveFormat(request);
        String body = readBody(request);

        try {
            Book book = parseBook(body, format);
            if (book.getId() <= 0) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Book id is required for update", format);
                return;
            }
            if (!isValidBook(book)) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing required fields: title and author are required", format);
                return;
            }
            bookDAO.updateBook(book);
            response.setStatus(HttpServletResponse.SC_OK);
            sendPlainMessage(response, "Book updated successfully", format);

        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Failed to parse request body: " + e.getMessage(), format);
        }
    }

    // -------------------------------------------------------------------------
    // DELETE — delete a book by id
    // -------------------------------------------------------------------------
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String format = resolveFormat(request);
        String idParam = request.getParameter("id");

        if (idParam == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "id parameter is required for delete", format);
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            bookDAO.deleteBook(id);
            response.setStatus(HttpServletResponse.SC_OK);
            sendPlainMessage(response, "Book deleted successfully", format);

        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid id parameter", format);
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage(), format);
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    /**
     * Resolves the desired response format from:
     * 1. ?format= query parameter
     * 2. Accept request header
     * Defaults to json if neither is set.
     */
    private String resolveFormat(HttpServletRequest request) {
        String formatParam = request.getParameter("format");
        if (formatParam != null) {
            return formatParam.toLowerCase().trim();
        }
        String accept = request.getHeader("Accept");
        if (accept != null) {
            if (accept.contains("application/xml")) return "xml";
            if (accept.contains("text/plain"))       return "text";
        }
        return "json"; // default
    }

    /**
     * Reads the full request body as a String.
     */
    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    /**
     * Parses a Book from the request body based on the resolved format.
     */
    private Book parseBook(String body, String format) throws JAXBException {
        if ("xml".equals(format)) {
            JAXBContext ctx = JAXBContext.newInstance(Book.class);
            Unmarshaller um = ctx.createUnmarshaller();
            return (Book) um.unmarshal(new StringReader(body));
        } else if ("text".equals(format)) {
            // TEXT format: comma-separated values
            // Expected: id,title,author,date,genres,characters,synopsis
            // id can be 0 for new books (POST)
            String[] parts = body.split(",", 7);
            Book b = new Book();
            if (parts.length >= 1) b.setId(Integer.parseInt(parts[0].trim()));
            if (parts.length >= 2) b.setTitle(parts[1].trim());
            if (parts.length >= 3) b.setAuthor(parts[2].trim());
            if (parts.length >= 4) b.setDate(parts[3].trim());
            if (parts.length >= 5) b.setGenres(parts[4].trim());
            if (parts.length >= 6) b.setCharacters(parts[5].trim());
            if (parts.length >= 7) b.setSynopsis(parts[6].trim());
            return b;
        } else {
            // Default: JSON
            return gson.fromJson(body, Book.class);
        }
    }

    /**
     * Sends a single Book in the requested format.
     */
    private void sendBookResponse(HttpServletResponse response, Book book, String format)
            throws IOException {
        setCorsHeaders(response);
        if ("xml".equals(format)) {
            response.setContentType("application/xml;charset=UTF-8");
            PrintWriter out = response.getWriter();
            try {
                JAXBContext ctx = JAXBContext.newInstance(Book.class);
                Marshaller m = ctx.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                m.marshal(book, out);
            } catch (JAXBException e) {
                out.print("<error>XML serialisation failed</error>");
            }
        } else if ("text".equals(format)) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().print(book.toString());
        } else {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print(gson.toJson(book));
        }
    }

    /**
     * Sends a collection of Books in the requested format.
     */
    private void sendBooksResponse(HttpServletResponse response, Collection<Book> books, String format)
            throws IOException {
        setCorsHeaders(response);
        if ("xml".equals(format)) {
            response.setContentType("application/xml;charset=UTF-8");
            PrintWriter out = response.getWriter();
            try {
                JAXBContext ctx = JAXBContext.newInstance(BookList.class);
                Marshaller m = ctx.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                m.marshal(new BookList(books), out);
            } catch (JAXBException e) {
                out.print("<error>XML serialisation failed</error>");
            }
        } else if ("text".equals(format)) {
            response.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = response.getWriter();
            for (Book b : books) {
                out.println(b.toString());
            }
        } else {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print(gson.toJson(books));
        }
    }

    /**
     * Sends a plain status message in the requested format.
     */
    private void sendPlainMessage(HttpServletResponse response, String message, String format)
            throws IOException {
        setCorsHeaders(response);
        if ("xml".equals(format)) {
            response.setContentType("application/xml;charset=UTF-8");
            response.getWriter().print("<message>" + message + "</message>");
        } else if ("text".equals(format)) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().print(message);
        } else {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"message\":\"" + message + "\"}");
        }
    }

    /**
     * Sends an error response in the requested format.
     */
    private void sendError(HttpServletResponse response, int status, String message, String format)
            throws IOException {
        response.setStatus(status);
        setCorsHeaders(response);
        if ("xml".equals(format)) {
            response.setContentType("application/xml;charset=UTF-8");
            response.getWriter().print("<error>" + message + "</error>");
        } else if ("text".equals(format)) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().print("ERROR: " + message);
        } else {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"error\":\"" + message + "\"}");
        }
    }

    /**
     * Adds CORS headers to allow the JS frontend to call this API.
     */
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
    }

    /**
     * Handles pre-flight OPTIONS requests from the browser (CORS).
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Basic server-side validation — title and author are required.
     */
    private boolean isValidBook(Book b) {
        return b != null
            && b.getTitle() != null && !b.getTitle().trim().isEmpty()
            && b.getAuthor() != null && !b.getAuthor().trim().isEmpty();
    }
}