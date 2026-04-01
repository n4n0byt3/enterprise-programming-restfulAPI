package model;

import java.util.Collection;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * JAXB wrapper class for a collection of Book objects.
 * Required because JAXB cannot directly serialise a plain List<Book>.
 * Wraps the list with a root XML element called <books>.
 */
@XmlRootElement(name = "books")
public class BookList {

    private Collection<Book> books;

    // Default no-arg constructor required by JAXB
    public BookList() {}

    public BookList(Collection<Book> books) {
        this.books = books;
    }

    @XmlElement(name = "book")
    public Collection<Book> getBooks() { return books; }
    public void setBooks(Collection<Book> books) { this.books = books; }
}