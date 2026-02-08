import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

class Student {
    final String id;
    final String name;
    int totalClasses;
    int attendedClasses;

    Student(String id, String name, int totalClasses, int attendedClasses) {
        this.id = id;
        this.name = name;
        this.totalClasses = totalClasses;
        this.attendedClasses = attendedClasses;
    }

    double getAttendanceRatio() {
        return totalClasses == 0 ? 1.0 : (double) attendedClasses / totalClasses;
    }

    double getAttendancePercentage() {
        return getAttendanceRatio() * 100.0;
    }

    void recordDailyAttendance(boolean present) {
        totalClasses++;
        if (present) {
            attendedClasses++;
        }
    }
}

class NotificationService {
    void notifyLowAttendance(Student student, double threshold) {
        System.out.println("Notify student " + student.name + " (" + student.id + "): attendance at "
                + String.format(Locale.US, "%.2f%%", student.getAttendancePercentage())
                + " is below " + String.format(Locale.US, "%.0f%%", threshold * 100));
        System.out.println("Notify advisor: follow up with " + student.name + " (" + student.id + ")");
    }
}

class AdvisorDashboard {
    void showSummary(List<Student> students) {
        System.out.println("\n--- Advisor Attendance Summary ---");
        students.stream()
                .sorted(Comparator.comparingDouble(Student::getAttendanceRatio))
                .forEach(student -> System.out.println(student.id + " (" + student.name + ") -> "
                        + String.format(Locale.US, "%.2f%%", student.getAttendancePercentage())));
    }
}

public class project {
    private static final double ALERT_THRESHOLD = 0.75;
    private static final NotificationService NOTIFIER = new NotificationService();
    private static final AdvisorDashboard DASHBOARD = new AdvisorDashboard();

    private static boolean authenticate(Scanner scanner) {
                System.out.println("=========================================");
        System.out.println("Welcome to Attendance Management System");
        System.out.println("=========================================");
        System.out.println("=== Login ===");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        if ("admin".equals(username) && "admin".equals(password)) {
            System.out.println("Login successful.\n");
            return true;
        }
        System.out.println("Access denied.");
        return false;
    }

    private static void displayMenu() {

        System.out.println("=== Attendance Menu ===");
        System.out.println("1. Add student details");
        System.out.println("2. View all attendance");
        System.out.println("3. View specific student attendance");
        System.out.println("4. Record daily attendance for all students");
        System.out.println("5. View students below 75% attendance");
        System.out.println("6. Save attendance to file");
        System.out.println("0. Exit");
        System.out.print("Choose option: ");
    }

    private static void handleAddStudent(Scanner scanner, List<Student> roster) {
        System.out.print("Student name: ");
        String name = scanner.nextLine();
        System.out.print("Student ID: ");
        String id = scanner.nextLine();
        System.out.print("Total classes conducted: ");
        int total = Integer.parseInt(scanner.nextLine());
        System.out.print("Classes attended so far: ");
        int attended = Integer.parseInt(scanner.nextLine());

        Student student = new Student(id, name, total, attended);
        roster.add(student);

        System.out.println("Current attendance for " + name + " (" + id + "): "
                + String.format(Locale.US, "%.2f%%", student.getAttendancePercentage()));

        if (student.getAttendanceRatio() < ALERT_THRESHOLD) {
            NOTIFIER.notifyLowAttendance(student, ALERT_THRESHOLD);
        }
        System.out.println();
    }

    private static void showAllAttendance(List<Student> roster) {
        if (roster.isEmpty()) {
            System.out.println("No student records available.\n");
            return;
        }
        DASHBOARD.showSummary(roster);
        System.out.println();
    }

    private static void showSpecificAttendance(Scanner scanner, List<Student> roster) {
        if (roster.isEmpty()) {
            System.out.println("No student records available.\n");
            return;
        }
        System.out.print("Enter student ID: ");
        String id = scanner.nextLine();
        Student student = roster.stream()
                .filter(entry -> entry.id.equals(id))
                .findFirst()
                .orElse(null);
        if (student == null) {
            System.out.println("Student " + id + " not found.\n");
            return;
        }
        System.out.println(student.name + " (" + student.id + ") attendance: "
                + String.format(Locale.US, "%.2f%%", student.getAttendancePercentage()) + "\n");
    }

    private static void handleDailyAttendance(Scanner scanner, List<Student> roster) {
        if (roster.isEmpty()) {
            System.out.println("No student records available.\n");
            return;
        }
        System.out.println("=== Record Daily Attendance ===");
        roster.stream()
                .sorted(Comparator.comparing(student -> student.id))
                .forEach(student -> {
                    while (true) {
                        System.out.print("Was student " + student.name + " (" + student.id + ") present? (y/n): ");
                        String input = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
                        if ("y".equals(input)) {
                            student.recordDailyAttendance(true);
                            break;
                        }
                        if ("n".equals(input)) {
                            student.recordDailyAttendance(false);
                            break;
                        }
                        System.out.println("Enter y or n.");
                    }
                });
        System.out.println("Attendance recorded.\n");
    }

    private static void showLowAttendance(List<Student> roster) {
        if (roster.isEmpty()) {
            System.out.println("No student records available.\n");
            return;
        }
        System.out.println("=== Students Below 75% Attendance ===");
        roster.stream()
                .filter(student -> student.getAttendanceRatio() < ALERT_THRESHOLD)
                .sorted(Comparator.comparing(student -> student.id))
                .forEach(student -> System.out.println(student.name + " (" + student.id + ") -> "
                        + String.format(Locale.US, "%.2f%%", student.getAttendancePercentage())));
        System.out.println();
    }

    private static void saveAttendanceToFile(Scanner scanner, List<Student> roster) {
        if (roster.isEmpty()) {
            System.out.println("No student records available.\n");
            return;
        }
        System.out.print("Enter output file path: ");
        String target = scanner.nextLine().trim();
        if (target.isEmpty()) {
            System.out.println("File path cannot be empty.\n");
            return;
        }

        Path path = Paths.get(target);
        List<String> lines = roster.stream()
            .sorted(Comparator.comparing(student -> student.id))
            .map(student -> student.id + "\t" + student.name + "\t"
                + String.format(Locale.US, "%.2f%%", student.getAttendancePercentage()))
            .collect(Collectors.toList());

        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.write(path, lines, StandardCharsets.UTF_8);
            System.out.println("Attendance saved to " + path.toAbsolutePath() + "\n");
        } catch (IOException ex) {
            System.out.println("Failed to write file: " + ex.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        if (!authenticate(scanner)) {
            scanner.close();
            return;
        }

        List<Student> roster = new ArrayList<>();
        boolean running = true;

        while (running) {
            displayMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleAddStudent(scanner, roster);
                    break;
                case "2":
                    showAllAttendance(roster);
                    break;
                case "3":
                    showSpecificAttendance(scanner, roster);
                    break;
                case "4":
                    handleDailyAttendance(scanner, roster);
                    break;
                case "5":
                    showLowAttendance(roster);
                    break;
                case "6":
                    saveAttendanceToFile(scanner, roster);
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option.\n");
            }
        }

        System.out.println("THANK YOU.");
        scanner.close();
    }
}
