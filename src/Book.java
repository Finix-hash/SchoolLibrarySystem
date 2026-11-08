/**
 * A title in the inventory. {@code availableCopies} counts copies that are
 * currently on the shelf (not the total number owned).
 */
public class Book {
    private String title;
    private String author;
    private String isbn;
    private int availableCopies;

    public Book(String title, String author, String isbn, int availableCopies) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.availableCopies = availableCopies;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public int getAvailableCopies() { return availableCopies; }

    public boolean borrowCopy() {
        if (availableCopies > 0) {
            availableCopies--;
            return true;
        }
        return false;
    }

    public void returnCopy() {
        availableCopies++;
    }

    /** Used when staff add more copies of an ISBN that already exists. */
    public void addCopies(int count) {
        if (count > 0) {
            availableCopies += count;
        }
    }
}
