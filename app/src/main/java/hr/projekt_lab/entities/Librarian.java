package hr.projekt_lab.entities;

public class Librarian {
    private Long id;
    private String username;
    private String passwordHash;
    private String fullName;

    public Librarian() {
    }

    public Librarian(String username, String passwordHash, String fullName) {
        this(null, username, passwordHash, fullName);
    }

    public Librarian(Long id, String username, String passwordHash, String fullName) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String toString() {
        return fullName == null || fullName.isBlank() ? username : fullName;
    }
}