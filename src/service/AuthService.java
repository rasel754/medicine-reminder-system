package service;

import dao.UserDAO;
import model.User;

/**
 * AuthService handles user session state and validates logins for ADMIN or USER roles.
 */
public class AuthService {
    private final UserDAO userDAO;
    private static User currentUser = null;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Authenticates the user and returns their role if valid.
     *
     * @param username user input name
     * @param password user input password
     * @return String role ('ADMIN' or 'USER') or null if authentication fails
     */
    public String authenticateAndGetRole(String username, String password) {
        User user = userDAO.login(username, password);
        if (user != null) {
            currentUser = user;
            return user.getRole();
        }
        return null;
    }

    /**
     * Registers a new user into the database.
     */
    public boolean register(String username, String password, String role) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return false;
        }
        User newUser = new User(0, username, password, role);
        return userDAO.registerUser(newUser) != null;
    }

    /**
     * Checks if a username is already taken.
     */
    public boolean usernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return userDAO.usernameExists(username.trim());
    }

    public void logout() {
        currentUser = null;
    }
}
