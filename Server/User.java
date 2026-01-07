package Server;

public class User {
    public final String username;
    public final String password;
    public final boolean admin;

    public User(String username, String password, boolean admin) {
        this.username = username;
        this.password = password;
        this.admin = admin;
    }
}
