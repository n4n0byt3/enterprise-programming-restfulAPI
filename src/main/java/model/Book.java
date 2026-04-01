package model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Book model class representing a book record from the database.
 * JAXB annotations added for XML serialisation support.
 * Fields match the assignment SQL schema exactly.
 */
@XmlRootElement(name = "book")
public class Book {

    private int id;
    private String title;
    private String author;
    private String date;       // Format: MM/DD/YY or MM/DD/YYYY
    private String genres;     // Comma-separated
    private String characters; // Comma-separated, optional
    private String synopsis;   // Optional

    // Default no-arg constructor required by JAXB
    public Book() {}

    // Full constructor
    public Book(int id, String title, String author, String date,
                String genres, String characters, String synopsis) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.date = date;
        this.genres = genres;
        this.characters = characters;
        this.synopsis = synopsis;
    }

    // --- Getters and Setters with JAXB @XmlElement annotations ---

    @XmlElement
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @XmlElement
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @XmlElement
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    @XmlElement
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    @XmlElement
    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }

    @XmlElement
    public String getCharacters() { return characters; }
    public void setCharacters(String characters) { this.characters = characters; }

    @XmlElement
    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

    /**
     * Plain text representation of a book for TEXT format responses.
     */
    @Override
    public String toString() {
        return "ID: " + id
            + " | Title: " + title
            + " | Author: " + author
            + " | Date: " + date
            + " | Genres: " + genres
            + " | Characters: " + characters
            + " | Synopsis: " + synopsis;
    }
}