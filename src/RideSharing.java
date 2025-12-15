import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;


enum DriverStatus { NONE, PENDING, APPROVED, REJECTED }
enum RideType { INSTANT, SCHEDULED }
enum RideStatus { OPEN, BOOKED, COMPLETED, CANCELLED }

class RideRequest {
    String from;
    String to;
    String time;
    User student;

    public RideRequest(String from, String to, String time, User student) {
        this.from = from;
        this.to = to;
        this.time = time;
        this.student = student;
    }

    @Override
    public String toString() {
        return "<html><b>" + from + " -> " + to + "</b><br>Time: " + time + " | Requested by: " + student.getName() + "</html>";
    }
}


class User {
    private String id;
    private String name;
    private String password;
    private String phone;
    
    private DriverStatus driverStatus = DriverStatus.NONE;
    private String licenseId;
    private String iqamaId;
    private String birthDate;
    private String carModel;
    private List<Integer> ratings = new ArrayList<>();

    public User(String id, String name, String password, String phone) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.phone = phone;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getId() { return id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean checkPassword(String pass) { return password.equals(pass); }
    public void setPassword(String password) { this.password = password; }
    public DriverStatus getDriverStatus() { return driverStatus; }
    public void setDriverStatus(DriverStatus status) { this.driverStatus = status; }
    
    public void submitDriverDocs(String license, String iqama, String dob, String car) {
        this.licenseId = license;
        this.iqamaId = iqama;
        this.birthDate = dob;
        this.carModel = car;
        this.driverStatus = DriverStatus.PENDING; 
    }

    public void approveDriver() { this.driverStatus = DriverStatus.APPROVED; }
    public void rejectDriver() { this.driverStatus = DriverStatus.REJECTED; }

    public void addRating(int r) { ratings.add(r); }
    public double getAverageRating() { return ratings.isEmpty() ? 0.0 : ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0); }

    public String getDriverDetails() {
        return "<html><b>Name:</b> " + name + 
               "<br><b>Phone:</b> " + phone +
               "<br><b>License:</b> " + licenseId + 
               "<br><b>ID/Iqama:</b> " + iqamaId + 
               "<br><b>Car:</b> " + carModel +
               "<br><b>Average Rating:</b> " + String.format("%.1f", getAverageRating()) + "/5</html>";
    }
}

class Ride {
    String driverName;
    String driverPhone;
    String from;
    String to;
    double cost;
    RideType type;
    String dateTime;
    RideStatus status = RideStatus.OPEN;
    User passenger; 
    int rating = 0;
    String review = ""; 

    public Ride(String driverName, String driverPhone, String from, String to, double cost, RideType type, String dateTime) {
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.from = from;
        this.to = to;
        this.cost = cost;
        this.type = type;
        this.dateTime = dateTime;
    }

    public void bookRide(User p) {
        this.passenger = p;
        this.status = RideStatus.BOOKED;
    }

    public void cancelBooking() {
        this.passenger = null;
        this.status = RideStatus.OPEN;
    }

    public void finishRide() {
        this.status = RideStatus.COMPLETED;
    }

    @Override
    public String toString() {
        String typeIcon = (type == RideType.INSTANT) ? "NOW" : "LATER";
        return "<html><b style='font-size:110%'>[" + typeIcon + "] " + from + " -> " + to + "</b><br>" + 
               "<span style='color:gray'>" + dateTime + " | " + cost + " SAR | Driver: " + driverName + "</span></html>";
    }

    public String getDriverViewString() {
        String stateHtml = "<span style='color:orange'>Waiting...</span>";
        if (status == RideStatus.BOOKED) {
            stateHtml = "<span style='color:cyan'>BOOKED by: " + passenger.getName() + " | Ph: " + passenger.getPhone() + "</span>";
        } else if (status == RideStatus.COMPLETED) {
            stateHtml = "<span style='color:green'>FINISHED (Paid)</span>";
            if (rating > 0) {
                stateHtml += "<br><span style='color:blue'>Rating: " + rating + "/5</span>";
            }
            if (!review.isEmpty()) {
                stateHtml += "<br><span style='color:purple'>Review: " + review + "</span>";
            }
        }
        return "<html><b>" + from + " -> " + to + "</b> (" + dateTime + ")<br>" + stateHtml + "</html>";
    }

    public String getStudentViewString() {
        String stateHtml = "";
        if (status == RideStatus.BOOKED) {
            stateHtml = "<span style='color:cyan'>Booked - Call Driver: " + driverPhone + "</span>";
        } else if (status == RideStatus.COMPLETED) {
            stateHtml = "<span style='color:green'>FINISHED & PAID</span>";
        }
        return "<html><b>" + from + " -> " + to + "</b> (" + cost + " SAR)<br>" + stateHtml + "</html>";
    }
}

public class RideSharing extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    
    private final Color IMAMU_GREEN = new Color(0, 153, 76); 
    private final Color IMAMU_BLUE = new Color(51, 102, 204);
    private final Color ADMIN_RED = new Color(204, 0, 0);

    private List<User> users = new ArrayList<>();
    private List<Ride> allRides = new ArrayList<>(); 
    private List<RideRequest> rideRequests = new ArrayList<>();
    private User currentUser = null;

    private final String ADMIN_ID = "admin";
    private final String ADMIN_PASS = "admin123";

    private DefaultListModel<String> findRideModel = new DefaultListModel<>();
    private DefaultListModel<User> pendingDriversModel = new DefaultListModel<>();
    private DefaultListModel<String> adminHistoryModel = new DefaultListModel<>();
    private DefaultListModel<String> driverMyRidesModel = new DefaultListModel<>();
    private DefaultListModel<String> studentMyRidesModel = new DefaultListModel<>();

    private JTextField loginIdField;
    private JPasswordField loginPassField;
    private JComboBox<String> roleSelector;
    private JTextField regIdField, regNameField, regPhoneField;
    private JPasswordField regPassField;
    private JComboBox<String> regRole;

    public RideSharing() {
        setTitle("Ride-Sharing App");
        setSize(480, 800); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 

        User demoDriver = new User("44110001", "Khalid", "Pass123!", "0551112222");
        demoDriver.submitDriverDocs("LIC-99", "1112223334", "01/01/2000", "Camry");
        demoDriver.approveDriver(); 
        users.add(demoDriver);

        User demoStudent = new User("44110002", "Ahmed", "Pass123!", "0559998888");
        users.add(demoStudent);
        
        allRides.add(new Ride(demoDriver.getName(), demoDriver.getPhone(), "Al-Nakhil Mall", "IMAMU Gate 1", 25.0, RideType.INSTANT, "NOW"));
        allRides.add(new Ride(demoDriver.getName(), demoDriver.getPhone(), "Airport", "IMAMU Housing", 40.0, RideType.SCHEDULED, "15-12-2025 09:00 AM"));
        
        refreshLists();

        mainPanel.add(createLoginPanel(), "Login");
        mainPanel.add(createRegisterPanel(), "Register");
        mainPanel.add(createStudentDashboard(), "StudentDash");
        mainPanel.add(createDriverDashboard(), "DriverDash");
        mainPanel.add(createAdminDashboard(), "AdminDash");
        mainPanel.add(createVerificationPanel(), "Verification");
        mainPanel.add(createFindRidePanel(), "FindRide");
        mainPanel.add(createOfferRidePanel(), "OfferRide");
        mainPanel.add(createAdminHistoryPanel(), "AdminHistory");
        mainPanel.add(createDriverMyRidesPanel(), "DriverHistory");
        mainPanel.add(createStudentMyRidesPanel(), "StudentHistory"); 
        mainPanel.add(createProfilePanel(), "Profile");
        mainPanel.add(createRequestRidePanel(), "RequestRide");

        add(mainPanel);
        cardLayout.show(mainPanel, "Login");
    }
    
    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        if (bg != null) {
            btn.setBackground(bg);
            btn.setForeground(Color.WHITE);
        }
        return btn;
    }
    
    private void refreshLists() {
        findRideModel.clear();
        driverMyRidesModel.clear();
        studentMyRidesModel.clear();
        adminHistoryModel.clear();

        for (Ride r : allRides) {
            if (r.status == RideStatus.OPEN) {
                findRideModel.addElement(r.toString());
            }
            adminHistoryModel.addElement(r.getDriverViewString());
            
            if (currentUser != null) {
                if (r.driverName.equals(currentUser.getName())) {
                    driverMyRidesModel.addElement(r.getDriverViewString());
                }
                if (r.passenger != null && r.passenger.getId().equals(currentUser.getId())) {
                    studentMyRidesModel.addElement(r.getStudentViewString());
                }
            }
        }

        pendingDriversModel.clear();
        for (User u : users) {
            if (u.getDriverStatus() == DriverStatus.PENDING) pendingDriversModel.addElement(u);
        }
    }

    private boolean isAlpha(String text) { return text.matches("[a-zA-Z\\s]+"); }
    private boolean isNumeric(String text) { return text.matches("[0-9]+"); }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Ride-Sharing App", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(IMAMU_BLUE);

        loginIdField = new JTextField(15);
        loginPassField = new JPasswordField(15);
        String[] roles = { "Student Login", "Driver Login", "Admin Login" };
        roleSelector = new JComboBox<>(roles);
        
        JButton loginBtn = createButton("Login", IMAMU_GREEN);
        JButton regBtn = createButton("Create New Account", IMAMU_BLUE);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(title, gbc);
        gbc.gridy = 1; panel.add(new JLabel("Select Role:"), gbc);
        gbc.gridy = 2; panel.add(roleSelector, gbc);
        gbc.gridy = 3; panel.add(new JLabel("ID / Username:"), gbc);
        gbc.gridy = 4; panel.add(loginIdField, gbc);
        gbc.gridy = 5; panel.add(new JLabel("Password:"), gbc);
        gbc.gridy = 6; panel.add(loginPassField, gbc);
        gbc.gridy = 7; panel.add(Box.createVerticalStrut(15), gbc);
        gbc.gridy = 8; panel.add(loginBtn, gbc);
        gbc.gridy = 9; panel.add(regBtn, gbc);

        loginBtn.addActionListener(e -> attemptLogin());
        regBtn.addActionListener(e -> cardLayout.show(mainPanel, "Register"));
        return panel;
    }
    
    private void attemptLogin() {
        String role = (String) roleSelector.getSelectedItem();
        String id = loginIdField.getText();
        String pass = new String(loginPassField.getPassword());

        if (role.equals("Admin Login")) {
            if (id.equals(ADMIN_ID) && pass.equals(ADMIN_PASS)) {
                cardLayout.show(mainPanel, "AdminDash");
                refreshLists();
            } else { JOptionPane.showMessageDialog(this, "Invalid Admin Credentials"); }
            return;
        }

        User foundUser = null;
        for (User u : users) {
            if (u.getId().equals(id) && u.checkPassword(pass)) { foundUser = u; break; }
        }

        if (foundUser == null) { JOptionPane.showMessageDialog(this, "User not found or wrong password."); return; }
        currentUser = foundUser;

        if (role.equals("Driver Login")) {
            if (currentUser.getDriverStatus() == DriverStatus.APPROVED) cardLayout.show(mainPanel, "DriverDash");
            else if (currentUser.getDriverStatus() == DriverStatus.PENDING) JOptionPane.showMessageDialog(this, "Pending Admin Approval.");
            else if (currentUser.getDriverStatus() == DriverStatus.REJECTED) JOptionPane.showMessageDialog(this, "Rejected. Login as Student to re-apply.");
            else JOptionPane.showMessageDialog(this, "Not a driver. Login as Student to apply.");
        } else {
            cardLayout.show(mainPanel, "StudentDash");
        }
        refreshLists();
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 20, 8, 20); 

        JLabel title = new JLabel("Create Account", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        regIdField = new JTextField(15); 
        regNameField = new JTextField(15); 
        regPhoneField = new JTextField(15);
        regPassField = new JPasswordField(15);
        regRole = new JComboBox<>(new String[]{"Student", "Driver Applicant"});
        
        JButton doRegBtn = createButton("Register", IMAMU_GREEN);
        JButton backBtn = createButton("Back", Color.GRAY);
        
        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=2; panel.add(title, gbc);
        gbc.gridy=1; gbc.gridwidth=1; panel.add(new JLabel("ID:"), gbc); gbc.gridx=1; panel.add(regIdField, gbc);
        gbc.gridx=0; gbc.gridy=2; panel.add(new JLabel("Name (Letters Only):"), gbc); gbc.gridx=1; panel.add(regNameField, gbc);
        gbc.gridx=0; gbc.gridy=3; panel.add(new JLabel("Phone (05...):"), gbc); gbc.gridx=1; panel.add(regPhoneField, gbc);
        gbc.gridx=0; gbc.gridy=4; panel.add(new JLabel("Password:"), gbc); gbc.gridx=1; panel.add(regPassField, gbc);
        gbc.gridx=0; gbc.gridy=5; panel.add(new JLabel("Role:"), gbc); gbc.gridx=1; panel.add(regRole, gbc);
        gbc.gridx=0; gbc.gridy=6; gbc.gridwidth=2; panel.add(Box.createVerticalStrut(20), gbc);

        gbc.gridy=7; JPanel btnPanel = new JPanel(new GridLayout(1, 2, 15, 0)); 
        btnPanel.add(backBtn); btnPanel.add(doRegBtn);
        panel.add(btnPanel, gbc);

        doRegBtn.addActionListener(e -> {
            String name = regNameField.getText();
            String phone = regPhoneField.getText();
            String pass = new String(regPassField.getPassword());

            if (!isAlpha(name)) { JOptionPane.showMessageDialog(this, "Name must contain letters only."); return; }
            if (!isNumeric(phone)) { JOptionPane.showMessageDialog(this, "Phone must contain numbers only."); return; }
            if (!isValidPassword(pass)) { JOptionPane.showMessageDialog(this, "Password Invalid! 8+ chars, Upper, Lower, Special."); return; }

            users.add(new User(regIdField.getText(), name, pass, phone));
            User newUser = users.get(users.size() - 1);
            if ("Driver Applicant".equals((String) regRole.getSelectedItem())) {
                newUser.setDriverStatus(DriverStatus.PENDING);
                JOptionPane.showMessageDialog(this, "Account Created! Login as Driver (pending admin approval).");
            } else {
                JOptionPane.showMessageDialog(this, "Account Created! Login as Student.");
            }
            cardLayout.show(mainPanel, "Login");
        });
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Login"));
        return panel;
    }

    private boolean isValidPassword(String p) {
        if (p.length() < 8) return false;
        if (!p.matches(".*[A-Z].*")) return false; 
        if (!p.matches(".*[a-z].*")) return false; 
        if (!p.matches(".*[!@#$%^&*()_+=<>?].*")) return false; 
        return true;
    }

    private JPanel createStudentDashboard() {
        JPanel panel = new JPanel(new GridLayout(8, 1, 15, 15));
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));
        JLabel header = new JLabel("Student Dashboard", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JButton findBtn = createButton("Find a Ride", IMAMU_GREEN);
        JButton requestBtn = createButton("Request a Ride", IMAMU_BLUE);
        JButton myRidesBtn = createButton("My Rides (Pay/Cancel)", IMAMU_BLUE); 
        JButton verifyBtn = createButton("Apply to become Driver", Color.DARK_GRAY);
        JButton emergencyBtn = createButton("Emergency Alert", Color.RED);
        JButton profileBtn = createButton("Edit Profile", Color.ORANGE);
        JButton logoutBtn = createButton("Logout", Color.GRAY);

        panel.add(header); panel.add(findBtn); panel.add(requestBtn); panel.add(myRidesBtn); panel.add(verifyBtn); panel.add(emergencyBtn); panel.add(profileBtn); panel.add(logoutBtn);

        findBtn.addActionListener(e -> { refreshLists(); cardLayout.show(mainPanel, "FindRide"); });
        requestBtn.addActionListener(e -> cardLayout.show(mainPanel, "RequestRide"));
        myRidesBtn.addActionListener(e -> { refreshLists(); cardLayout.show(mainPanel, "StudentHistory"); });
        verifyBtn.addActionListener(e -> {
            if (currentUser.getDriverStatus() == DriverStatus.APPROVED) JOptionPane.showMessageDialog(this, "You are already a driver!");
            else if (currentUser.getDriverStatus() == DriverStatus.PENDING) JOptionPane.showMessageDialog(this, "Application Pending.");
            else cardLayout.show(mainPanel, "Verification");
        });
        emergencyBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Emergency Alert Sent! Authorities have been notified."));
        profileBtn.addActionListener(e -> cardLayout.show(mainPanel, "Profile"));
        logoutBtn.addActionListener(e -> cardLayout.show(mainPanel, "Login"));
        return panel;
    }

    private JPanel createStudentMyRidesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("My Booked Rides", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setBorder(new EmptyBorder(10,0,10,0));

        JList<String> list = new JList<>(studentMyRidesModel);
        list.setFixedCellHeight(60);

        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        JButton payBtn = createButton("Pay & Finish", IMAMU_GREEN);
        JButton cancelBtn = createButton("Cancel Ride", Color.RED);
        JButton backBtn = createButton("Back", Color.GRAY);

        btnPanel.add(backBtn); btnPanel.add(cancelBtn); btnPanel.add(payBtn);

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        payBtn.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx == -1) return;
            Ride r = findRideByPassenger(currentUser, idx);
            if (r != null && r.status == RideStatus.BOOKED) {
                int result = JOptionPane.showOptionDialog(this, "Total: " + r.cost + " SAR. Select payment method:", "Payment", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Card", "Wallet", "Cash", "Cancel"}, "Card");
                if (result == 3 || result == JOptionPane.CLOSED_OPTION) return; // Cancel
                r.finishRide();
                String paymentMsg;
                if (result == 0) paymentMsg = "Card payment processed! Ride Finished.";
                else if (result == 1) paymentMsg = "Wallet payment processed! Ride Finished.";
                else paymentMsg = "Please pay the driver " + r.cost + " SAR in cash upon pickup. Ride Finished.";
                JOptionPane.showMessageDialog(this, paymentMsg);
                // Prompt for rating
                String ratingStr = JOptionPane.showInputDialog(this, "Rate your driver (1-5):");
                if (ratingStr != null && !ratingStr.trim().isEmpty()) {
                    try {
                        int rating = Integer.parseInt(ratingStr.trim());
                        if (rating >= 1 && rating <= 5) {
                            r.rating = rating;
                            // Find driver and add rating
                            for (User u : users) {
                                if (u.getName().equals(r.driverName)) {
                                    u.addRating(rating);
                                    break;
                                }
                            }
                            // Prompt for review
                            String review = JOptionPane.showInputDialog(this, "Leave a review for your driver (optional):");
                            if (review != null) {
                                r.review = review;
                            }
                        } else {
                            JOptionPane.showMessageDialog(this, "Rating must be between 1 and 5.");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid rating. Please enter a number.");
                    }
                }
                refreshLists();
            } else { JOptionPane.showMessageDialog(this, "Ride is already finished or invalid."); }
        });

        cancelBtn.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx == -1) return;
            Ride r = findRideByPassenger(currentUser, idx);
            if (r != null && r.status == RideStatus.BOOKED) {
                int confirm = JOptionPane.showConfirmDialog(this, "Cancel this ride?", "Cancel", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    r.cancelBooking();
                    JOptionPane.showMessageDialog(this, "Ride Cancelled.");
                    refreshLists();
                }
            } else { JOptionPane.showMessageDialog(this, "Cannot cancel finished rides."); }
        });

        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "StudentDash"));
        return panel;
    }

    private Ride findRideByPassenger(User u, int listIndex) {
        int count = 0;
        for (Ride r : allRides) {
            if (r.passenger != null && r.passenger.getId().equals(u.getId())) {
                if (count == listIndex) return r;
                count++;
            }
        }
        return null;
    }

    private JPanel createDriverDashboard() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 15, 15)); 
        panel.setBorder(new EmptyBorder(50, 40, 50, 40)); 
        
        JLabel header = new JLabel("Driver Dashboard", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JButton offerBtn = createButton("Offer a Ride", IMAMU_BLUE);
        JButton myRidesBtn = createButton("My Rides (See Students)", Color.DARK_GRAY);
        JButton emergencyBtn = createButton("Emergency Alert", Color.RED);
        JButton profileBtn = createButton("Edit Profile", Color.ORANGE);
        JButton logoutBtn = createButton("Logout", Color.GRAY);

        panel.add(header); 
        panel.add(offerBtn); 
        panel.add(myRidesBtn); 
        panel.add(emergencyBtn);
        panel.add(profileBtn);
        panel.add(logoutBtn);

        offerBtn.addActionListener(e -> cardLayout.show(mainPanel, "OfferRide"));
        myRidesBtn.addActionListener(e -> { refreshLists(); cardLayout.show(mainPanel, "DriverHistory"); });
        emergencyBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Emergency Alert Sent! Authorities have been notified."));
        profileBtn.addActionListener(e -> cardLayout.show(mainPanel, "Profile"));
        logoutBtn.addActionListener(e -> cardLayout.show(mainPanel, "Login"));
        return panel;
    }

    private JPanel createDriverMyRidesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("My Posted Rides", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setBorder(new EmptyBorder(10,0,10,0));

        JList<String> list = new JList<>(driverMyRidesModel);
        list.setFixedCellHeight(70); 

        JButton backBtn = createButton("Back", Color.GRAY);
        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        panel.add(backBtn, BorderLayout.SOUTH);
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "DriverDash"));
        return panel;
    }

    private JPanel createVerificationPanel() {
        JPanel panel = new JPanel(new GridBagLayout()); 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 20, 5, 20); 
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        Dimension fieldSize = new Dimension(280, 35);

        JTextField licenseField = new JTextField(); licenseField.setPreferredSize(fieldSize);
        JTextField iqamaField = new JTextField(); iqamaField.setPreferredSize(fieldSize);
        JTextField dobField = new JTextField(); dobField.setPreferredSize(fieldSize);
        JTextField carField = new JTextField(); carField.setPreferredSize(fieldSize);

        JLabel title = new JLabel("Driver Application", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        gbc.gridx = 0; gbc.gridy = 0; panel.add(title, gbc);

        gbc.gridy++; panel.add(new JLabel("License Number (Nums):"), gbc);
        gbc.gridy++; panel.add(licenseField, gbc);

        gbc.gridy++; panel.add(new JLabel("ID / Iqama Number (Nums):"), gbc);
        gbc.gridy++; panel.add(iqamaField, gbc);

        gbc.gridy++; panel.add(new JLabel("Birth Date (DD/MM/YYYY):"), gbc);
        gbc.gridy++; panel.add(dobField, gbc);

        gbc.gridy++; panel.add(new JLabel("Car Model:"), gbc);
        gbc.gridy++; panel.add(carField, gbc);

        gbc.gridy++; panel.add(Box.createVerticalStrut(15), gbc);

        gbc.gridy++; 
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JButton backBtn = createButton("Back", Color.GRAY); JButton submitBtn = createButton("Submit", IMAMU_GREEN);
        btnPanel.add(backBtn); btnPanel.add(submitBtn); 
        btnPanel.setPreferredSize(fieldSize); 
        panel.add(btnPanel, gbc);

        submitBtn.addActionListener(e -> {
            if(licenseField.getText().isEmpty()) { JOptionPane.showMessageDialog(this, "Empty Fields!"); return; }
            if(!isNumeric(licenseField.getText()) || !isNumeric(iqamaField.getText())) {
                JOptionPane.showMessageDialog(this, "License and ID must be NUMBERS ONLY."); return;
            }
            currentUser.submitDriverDocs(licenseField.getText(), iqamaField.getText(), dobField.getText(), carField.getText());
            JOptionPane.showMessageDialog(this, "Application Submitted!"); refreshLists(); cardLayout.show(mainPanel, "StudentDash");
        });
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "StudentDash"));
        return panel;
    }

   
    private JPanel createAdminDashboard() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 20, 20));
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel title = new JLabel("Admin Control Panel", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(ADMIN_RED);

        JButton appsBtn = createButton("1. Driver Applications", IMAMU_BLUE);
        JButton historyBtn = createButton("2. Ride History Records", Color.DARK_GRAY);
        JButton logoutBtn = createButton("Logout", Color.GRAY);

        panel.add(title); panel.add(appsBtn); panel.add(historyBtn); panel.add(logoutBtn);

        appsBtn.addActionListener(e -> showDriverAppsDialog());
        historyBtn.addActionListener(e -> cardLayout.show(mainPanel, "AdminHistory"));
        logoutBtn.addActionListener(e -> cardLayout.show(mainPanel, "Login"));
        return panel;
    }

    private void showDriverAppsDialog() {
        JList<User> list = new JList<>(pendingDriversModel);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                User u = (User) value; setText(u.getName() + " (ID: " + u.getId() + ")"); return this;
            }
        });
        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(list), "Select Driver to Verify", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION && list.getSelectedValue() != null) {
            User u = list.getSelectedValue();
            int action = JOptionPane.showOptionDialog(this, u.getDriverDetails(), "Approve?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Approve", "Reject", "Cancel"}, "Approve");
            if (action == 0) { u.approveDriver(); JOptionPane.showMessageDialog(this, "Approved!"); }
            else if (action == 1) { u.rejectDriver(); JOptionPane.showMessageDialog(this, "Rejected."); }
            refreshLists();
        }
    }

    
    private JPanel createAdminHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("All Ride Records", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setBorder(new EmptyBorder(10,0,10,0));

        JList<String> list = new JList<>(adminHistoryModel);
        JButton backBtn = createButton("Back to Admin Dashboard", Color.GRAY);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        panel.add(backBtn, BorderLayout.SOUTH);
        
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "AdminDash"));
        return panel;
    }

    
    private JPanel createFindRidePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Available Rides", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JList<String> list = new JList<>(findRideModel);
        list.setFixedCellHeight(60);
        
        JButton joinBtn = createButton("Book Ride", IMAMU_GREEN);
        JButton backBtn = createButton("Back", Color.GRAY);
        JPanel bot = new JPanel(new GridLayout(1,2)); bot.add(backBtn); bot.add(joinBtn);

        panel.add(title, BorderLayout.NORTH); panel.add(new JScrollPane(list), BorderLayout.CENTER); panel.add(bot, BorderLayout.SOUTH);

        joinBtn.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx != -1) {
                Ride r = null; int count = 0;
                for(Ride ride : allRides) {
                    if (ride.status == RideStatus.OPEN) {
                         if (count == idx) { r = ride; break; }
                         count++;
                    }
                }
                
                if (r != null) {
                    r.bookRide(currentUser); 
                    JOptionPane.showMessageDialog(this, "Booked! Please go to 'My Rides' to Pay or Cancel.\nDriver Phone: " + r.driverPhone);
                    refreshLists();
                    cardLayout.show(mainPanel, "StudentDash");
                }
            } else { JOptionPane.showMessageDialog(this, "Select a ride first."); }
        });
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "StudentDash"));
        return panel;
    }

    
    private JPanel createOfferRidePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 0, 5, 0); 
        gbc.anchor = GridBagConstraints.CENTER;

        Dimension fieldSize = new Dimension(280, 35);

        JLabel title = new JLabel("Post a Ride", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JTextField from = new JTextField(); from.setPreferredSize(fieldSize);
        JTextField to = new JTextField(); to.setPreferredSize(fieldSize);
        JTextField cost = new JTextField(); cost.setPreferredSize(fieldSize);
        JCheckBox isScheduled = new JCheckBox("Schedule for later?");
        JTextField timeField = new JTextField("e.g. 20-12-2025 4:00 PM");
        timeField.setPreferredSize(fieldSize);
        timeField.setEnabled(false);

        isScheduled.addActionListener(e -> timeField.setEnabled(isScheduled.isSelected()));

        gbc.gridy=0; panel.add(title, gbc);
        gbc.gridy++; panel.add(new JLabel("From:"), gbc);
        gbc.gridy++; panel.add(from, gbc);
        gbc.gridy++; panel.add(new JLabel("To:"), gbc);
        gbc.gridy++; panel.add(to, gbc);
        gbc.gridy++; panel.add(new JLabel("Cost (SAR):"), gbc);
        gbc.gridy++; panel.add(cost, gbc);
        gbc.gridy++; panel.add(isScheduled, gbc);
        gbc.gridy++; panel.add(timeField, gbc);

        gbc.gridy++; panel.add(Box.createVerticalStrut(10), gbc);

        gbc.gridy++;
        JPanel btnPanel = new JPanel(new GridLayout(1,2,10,0));
        JButton back = createButton("Back", Color.GRAY); JButton post = createButton("Post", IMAMU_BLUE);
        btnPanel.add(back); btnPanel.add(post);
        btnPanel.setPreferredSize(fieldSize);
        panel.add(btnPanel, gbc);

        post.addActionListener(e -> {
            try {
                String t = isScheduled.isSelected() ? timeField.getText() : "NOW";
                RideType type = isScheduled.isSelected() ? RideType.SCHEDULED : RideType.INSTANT;
                allRides.add(new Ride(currentUser.getName(), currentUser.getPhone(), from.getText(), to.getText(), Double.parseDouble(cost.getText()), type, t));
                refreshLists();
                JOptionPane.showMessageDialog(this, "Ride Posted!");
                cardLayout.show(mainPanel, "DriverDash");
            } catch(Exception ex) { JOptionPane.showMessageDialog(this, "Invalid Cost."); }
        });
        back.addActionListener(e -> cardLayout.show(mainPanel, "DriverDash"));
        return panel;
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 20, 8, 20);

        JLabel title = new JLabel("Edit Profile", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JTextField nameField = new JTextField(currentUser != null ? currentUser.getName() : "", 15);
        JTextField phoneField = new JTextField(currentUser != null ? currentUser.getPhone() : "", 15);
        JPasswordField passField = new JPasswordField(15);
        JPasswordField confirmPassField = new JPasswordField(15);

        JButton saveBtn = createButton("Save Changes", IMAMU_GREEN);
        JButton backBtn = createButton("Back", Color.GRAY);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; panel.add(title, gbc);
        gbc.gridy = 1; gbc.gridwidth = 1; panel.add(new JLabel("Name (Letters Only):"), gbc); gbc.gridx = 1; panel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Phone (05...):"), gbc); gbc.gridx = 1; panel.add(phoneField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("New Password:"), gbc); gbc.gridx = 1; panel.add(passField, gbc);
        gbc.gridx = 0; gbc.gridy = 4; panel.add(new JLabel("Confirm Password:"), gbc); gbc.gridx = 1; panel.add(confirmPassField, gbc);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; panel.add(Box.createVerticalStrut(20), gbc);
        gbc.gridy = 6; JPanel btnPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        btnPanel.add(backBtn); btnPanel.add(saveBtn);
        panel.add(btnPanel, gbc);

        saveBtn.addActionListener(e -> {
            if (currentUser == null) return;
            String name = nameField.getText();
            String phone = phoneField.getText();
            String pass = new String(passField.getPassword());
            String confirm = new String(confirmPassField.getPassword());

            if (!isAlpha(name)) { JOptionPane.showMessageDialog(this, "Name must contain letters only."); return; }
            if (!isNumeric(phone)) { JOptionPane.showMessageDialog(this, "Phone must contain numbers only."); return; }
            if (!pass.isEmpty() && !isValidPassword(pass)) { JOptionPane.showMessageDialog(this, "Password Invalid! 8+ chars, Upper, Lower, Special."); return; }
            if (!pass.equals(confirm)) { JOptionPane.showMessageDialog(this, "Passwords do not match."); return; }

            currentUser.setName(name);
            currentUser.setPhone(phone);
            if (!pass.isEmpty()) currentUser.setPassword(pass);
            JOptionPane.showMessageDialog(this, "Profile Updated!");
            cardLayout.show(mainPanel, currentUser.getDriverStatus() == DriverStatus.APPROVED ? "DriverDash" : "StudentDash");
        });
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, currentUser.getDriverStatus() == DriverStatus.APPROVED ? "DriverDash" : "StudentDash"));
        return panel;
    }

    private JPanel createRequestRidePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 0, 5, 0); 
        gbc.anchor = GridBagConstraints.CENTER;

        Dimension fieldSize = new Dimension(280, 35);

        JLabel title = new JLabel("Request a Ride", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JTextField from = new JTextField(); from.setPreferredSize(fieldSize);
        JTextField to = new JTextField(); to.setPreferredSize(fieldSize);
        JCheckBox isInstant = new JCheckBox("Request for now?");
        JTextField timeField = new JTextField("e.g. 20-12-2025 4:00 PM");
        timeField.setPreferredSize(fieldSize);
        timeField.setEnabled(false);

        isInstant.addActionListener(e -> timeField.setEnabled(!isInstant.isSelected()));

        gbc.gridy=0; panel.add(title, gbc);
        gbc.gridy++; panel.add(new JLabel("From:"), gbc);
        gbc.gridy++; panel.add(from, gbc);
        gbc.gridy++; panel.add(new JLabel("To:"), gbc);
        gbc.gridy++; panel.add(to, gbc);
        gbc.gridy++; panel.add(isInstant, gbc);
        gbc.gridy++; panel.add(timeField, gbc);

        gbc.gridy++; panel.add(Box.createVerticalStrut(10), gbc);

        gbc.gridy++;
        JPanel btnPanel = new JPanel(new GridLayout(1,2,10,0));
        JButton back = createButton("Back", Color.GRAY); JButton request = createButton("Post Request", IMAMU_BLUE);
        btnPanel.add(back); btnPanel.add(request);
        btnPanel.setPreferredSize(fieldSize);
        panel.add(btnPanel, gbc);

        request.addActionListener(e -> {
            String t = isInstant.isSelected() ? "NOW" : timeField.getText();
            rideRequests.add(new RideRequest(from.getText(), to.getText(), t, currentUser));
            JOptionPane.showMessageDialog(this, "Ride Request Posted!");
            cardLayout.show(mainPanel, "StudentDash");
        });
        back.addActionListener(e -> cardLayout.show(mainPanel, "StudentDash"));
        return panel;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new FlatDarkLaf()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new RideSharing().setVisible(true));
    }
}