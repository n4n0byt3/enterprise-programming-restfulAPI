package web.api;

import model.Book;
import service.BookService;
import util.FormatHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;

/**
 * RESTful API Servlet for the Book database.
 * Mapped to /Bookapi in web.xml — handles GET, POST, PUT, DELETE.
 *
 * This servlet is responsible for HTTP concerns only:
 * - Reading request parameters and body
 * - Setting response status and content type
 * - Delegating business logic to BookService
 * - Delegating format conversion to FormatHelper
 *
 * Format selection via ?format=json|xml|text or Accept header (default: json)
 *
 * GET    /Bookapi                -> all books
 * GET    /Bookapi?id=X           -> single book by id
 * GET    /Bookapi?search=term    -> search books
 * POST   /Bookapi                -> insert new book
 * PUT    /Bookapi                -> update existing book (id required in body)
 * DELETE /Bookapi?id=X           -> delete book by id
 */
public class BookApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private BookService bookService;

    @Override
    public void init() throws ServletException {
        bookService = new BookService();
    }

    // -------------------------------------------------------------------------
    // GET — retrieve all, by id, or by search term
    // -------------------------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String format      = FormatHelper.resolveFormat(
            request.getParameter("format"),
            request.getHeader("Accept")
        );
        String idParam     = request.getParameter("id");
        String searchParam = request.getParameter("search");

        try {
            if (idParam != null) {
                int id = Integer.parseInt(idParam);
                Book book = bookService.getBookById(id);
                if (book == null) {
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Book not found", format);
                    return;
                }
                sendResponse(response, HttpServletResponse.SC_OK,
                    FormatHelper.serialiseBook(book, format), format);

            } else if (searchParam != null) {
                Collection<Book> books = bookService.searchBooks(searchParam);
                sendResponse(response, HttpServletResponse.SC_OK,
                    FormatHelper.serialiseBooks(books, format), format);

            } else {
                Collection<Book> books = bookService.getAllBooks();
                sendResponse(response, HttpServletResponse.SC_OK,
                    FormatHelper.serialiseBooks(books, format), format);
            }

        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid id parameter", format);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Server error: " + e.getMessage(), format);
        }
    }

    // -------------------------------------------------------------------------
    // POST — insert a new book
    // -------------------------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String format = FormatHelper.resolveFormat(
            request.getParameter("format"),
            request.getHeader("Accept")
        );
        String body = readBody(request);

        try {
            Book book = FormatHelper.deserialiseBook(body, format);
            bookService.insertBook(book);
            sendResponse(response, HttpServletResponse.SC_CREATED,
                formatMessage("Book inserted successfully", format), format);

        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage(), format);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                "Failed to parse request body: " + e.getMessage(), format);
        }
    }

    // -------------------------------------------------------------------------
    // PUT — update an existing book
    // -------------------------------------------------------------------------
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String format = FormatHelper.resolveFormat(
            request.getParameter("format"),
            request.getHeader("Accept")
        );
        String body = readBody(request);

        try {
            Book book = FormatHelper.deserialiseBook(body, format);
            bookService.updateBook(book);
            sendResponse(response, HttpServletResponse.SC_OK,
                formatMessage("Book updated successfully", format), format);

        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage(), format);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                "Failed to parse request body: " + e.getMessage(), format);
        }
    }

    // -------------------------------------------------------------------------
    // DELETE — delete a book by id
    // -------------------------------------------------------------------------
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String format = FormatHelper.resolveFormat(
            request.getParameter("format"),
            request.getHeader("Accept")
        );
        String idParam = request.getParameter("id");

        if (idParam == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                "id parameter is required for delete", format);
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            bookService.deleteBook(id);
            sendResponse(response, HttpServletResponse.SC_OK,
                formatMessage("Book deleted successfully", format), format);

        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid id parameter", format);
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage(), format);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Server error: " + e.getMessage(), format);
        }
    }

    // -------------------------------------------------------------------------
    // OPTIONS — CORS preflight
    // -------------------------------------------------------------------------
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Reads the full HTTP request body as a String.
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
     * Sends a successful response with the correct content type and CORS headers.
     */
    private void sendResponse(HttpServletResponse response, int status,
                               String body, String format) throws IOException {
        setCorsHeaders(response);
        response.setStatus(status);
        response.setContentType(FormatHelper.getContentType(format));
        response.getWriter().print(body);
    }

    /**
     * Sends an error response in the requested format.
     */
    private void sendError(HttpServletResponse response, int status,
                            String message, String format) throws IOException {
        setCorsHeaders(response);
        response.setStatus(status);
        response.setContentType(FormatHelper.getContentType(format));
        if ("xml".equals(format)) {
            response.getWriter().print("<error>" + message + "</error>");
        } else if ("text".equals(format)) {
            response.getWriter().print("ERROR: " + message);
        } else {
            response.getWriter().print("{\"error\":\"" + message + "\"}");
        }
    }

    /**
     * Formats a plain status message in the requested format.
     */
    private String formatMessage(String message, String format) {
        if ("xml".equals(format))  return "<message>" + message + "</message>";
        if ("text".equals(format)) return message;
        return "{\"message\":\"" + message + "\"}";
    }

    /**
     * Adds CORS headers to allow the JS frontend to call this API from the browser.
     */
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
    }
}