package util;

import model.Book;
import model.BookList;
import com.google.gson.Gson;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;

/**
 * Utility class responsible for all data format conversions.
 * Handles serialisation (Java -> JSON/XML/TEXT) and
 * deserialisation (JSON/XML/TEXT -> Java) for Book objects.
 *
 * Supported formats: json, xml, text
 * Used by BookApiServlet to keep HTTP handling separate from format logic.
 */
public class FormatHelper {

    private static final Gson gson = new Gson();

    // -------------------------------------------------------------------------
    // Serialisation — Book/Collection -> String
    // -------------------------------------------------------------------------

    /**
     * Serialises a single Book to the requested format string.
     * @param book   the Book to serialise
     * @param format "json", "xml", or "text"
     * @return formatted string representation
     */
    public static String serialiseBook(Book book, String format) {
        if ("xml".equals(format)) {
            return bookToXml(book);
        } else if ("text".equals(format)) {
            return book.toString();
        } else {
            return gson.toJson(book);
        }
    }

    /**
     * Serialises a collection of Books to the requested format string.
     * @param books  the collection of Books to serialise
     * @param format "json", "xml", or "text"
     * @return formatted string representation
     */
    public static String serialiseBooks(Collection<Book> books, String format) {
        if ("xml".equals(format)) {
            return bookListToXml(books);
        } else if ("text".equals(format)) {
            StringBuilder sb = new StringBuilder();
            for (Book b : books) {
                sb.append(b.toString()).append("\n");
            }
            return sb.toString();
        } else {
            return gson.toJson(books);
        }
    }

    // -------------------------------------------------------------------------
    // Deserialisation — String -> Book
    // -------------------------------------------------------------------------

    /**
     * Deserialises a request body string into a Book object.
     * @param body   the raw request body
     * @param format "json", "xml", or "text"
     * @return parsed Book object
     * @throws Exception if parsing fails
     */
    public static Book deserialiseBook(String body, String format) throws Exception {
        if ("xml".equals(format)) {
            return xmlToBook(body);
        } else if ("text".equals(format)) {
            return textToBook(body);
        } else {
            return gson.fromJson(body, Book.class);
        }
    }

    // -------------------------------------------------------------------------
    // Content type resolution
    // -------------------------------------------------------------------------

    /**
     * Returns the appropriate HTTP Content-Type header value for a format.
     * @param format "json", "xml", or "text"
     * @return content type string
     */
    public static String getContentType(String format) {
        if ("xml".equals(format))  return "application/xml;charset=UTF-8";
        if ("text".equals(format)) return "text/plain;charset=UTF-8";
        return "application/json;charset=UTF-8";
    }

    /**
     * Resolves the desired format from a request parameter or Accept header.
     * Priority: ?format= param > Accept header > default (json)
     * @param formatParam value of ?format= query parameter (may be null)
     * @param acceptHeader value of Accept request header (may be null)
     * @return resolved format string: "json", "xml", or "text"
     */
    public static String resolveFormat(String formatParam, String acceptHeader) {
        if (formatParam != null) {
            return formatParam.toLowerCase().trim();
        }
        if (acceptHeader != null) {
            if (acceptHeader.contains("application/xml")) return "xml";
            if (acceptHeader.contains("text/plain"))      return "text";
        }
        return "json";
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Marshals a single Book to an XML string using JAXB.
     */
    private static String bookToXml(Book book) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(Book.class);
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter sw = new StringWriter();
            m.marshal(book, sw);
            return sw.toString();
        } catch (JAXBException e) {
            return "<error>XML serialisation failed: " + e.getMessage() + "</error>";
        }
    }

    /**
     * Marshals a collection of Books to an XML string using JAXB.
     */
    private static String bookListToXml(Collection<Book> books) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(BookList.class);
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter sw = new StringWriter();
            m.marshal(new BookList(books), sw);
            return sw.toString();
        } catch (JAXBException e) {
            return "<error>XML serialisation failed: " + e.getMessage() + "</error>";
        }
    }

    /**
     * Unmarshals an XML string into a Book using JAXB.
     */
    private static Book xmlToBook(String xml) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(Book.class);
        Unmarshaller um = ctx.createUnmarshaller();
        return (Book) um.unmarshal(new StringReader(xml));
    }

    /**
     * Parses a comma-separated text string into a Book.
     * Expected format: id,title,author,date,genres,characters,synopsis
     * id should be 0 for new books (POST).
     */
    private static Book textToBook(String text) {
        String[] parts = text.split(",", 7);
        Book b = new Book();
        if (parts.length >= 1) b.setId(Integer.parseInt(parts[0].trim()));
        if (parts.length >= 2) b.setTitle(parts[1].trim());
        if (parts.length >= 3) b.setAuthor(parts[2].trim());
        if (parts.length >= 4) b.setDate(parts[3].trim());
        if (parts.length >= 5) b.setGenres(parts[4].trim());
        if (parts.length >= 6) b.setCharacters(parts[5].trim());
        if (parts.length >= 7) b.setSynopsis(parts[6].trim());
        return b;
    }
}