import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

// MAIN GUI APPLICATION CLASS :3
public class EmployeeManagementGUI extends JFrame {
    // Central Data Structures (ArrayList & HashMap)
    private final ArrayList<Employee> employeeList = new ArrayList<>();
    private final ArrayList<PayrollRecord> payrollRecords = new ArrayList<>();
    private final HashMap<String, Double> jobDeductionRules = new HashMap<>();

    // UI Tables & Models :3
    private DefaultTableModel empTableModel;
    private JTable empTable;
    private JPanel payrollReceiptList;
    private TableRowSorter<DefaultTableModel> employeeSorter;
    private JTextField txtSearchEmployee;

    // Form Inputs GUI components :3
    private JTextField txtId, txtName, txtSalary, txtOvertime;
    private JTextField txtPosition; //new field to display the position
    private JComboBox<String> comboJobType;
    private JComboBox<String> comboEmployeeSelect;
    private JCheckBox chkLetterSubmitted;
    private static final File PAYROLL_FILE = new File("Employee Generator Payslip.txt");
    private static final File EMPLOYEE_FILE = new File("EmployeeFILES.txt");

    public EmployeeManagementGUI() {
        setTitle("Employee Management System");
        setSize(900, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));

        // Rules for job deduction :3
        jobDeductionRules.put("Manager", 0.08); // 8% deduction for Manager
        jobDeductionRules.put("Developer", 0.05); // 5% deduction for Developer
        jobDeductionRules.put("Staff", 0.03); // 3% deduction for Staff

        // Create Main Tabbed Pane (Matches Architecture Sections in Documentation)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Employee Management", createEmployeePanel());
        tabbedPane.add("Payroll Processing", createPayrollPanel());
        tabbedPane.add("Payroll Summary Records", createSummaryPanel());

        add(tabbedPane);
        loadEmployees();
        loadPayrollData();
        refreshEmployeeDropdown();
        refreshEmployeeTable();
    }

    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel inputPanel = new JPanel(new GridLayout(7, 1, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add / Remove Employee"));

        inputPanel.add(new JLabel("Employee ID:"));
        txtId = new JTextField();
        inputPanel.add(txtId);

        inputPanel.add(new JLabel("Full Name:"));
        txtName = new JTextField();
        inputPanel.add(txtName);

        inputPanel.add(new JLabel("Job Position:"));
        comboJobType = new JComboBox<>(new String[] { "Manager", "Developer", "Staff" });
        inputPanel.add(comboJobType);

        inputPanel.add(new JLabel("Basic Salary (P):"));
        txtSalary = new JTextField();
        inputPanel.add(txtSalary);

        JButton btnAdd = new JButton("Add Employee");
        JButton btnRemove = new JButton("Delete Employee");
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(btnAdd);
        btnPanel.add(btnRemove);

        JPanel westContainer = new JPanel(new BorderLayout());
        westContainer.add(inputPanel, BorderLayout.CENTER);
        westContainer.add(btnPanel, BorderLayout.SOUTH);

        empTableModel = new DefaultTableModel(new String[] { "ID", "Name", "Job Position", "Basic Salary (P)" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table cells non-editable
            }
        };
        empTable = new JTable(empTableModel);

        // SEARCH FEATURE: Filters the employee table by ID, name, or job position while the user types.
        employeeSorter = new TableRowSorter<>(empTableModel);
        empTable.setRowSorter(employeeSorter);
        txtSearchEmployee = new JTextField(18);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search Employee:"));
        searchPanel.add(txtSearchEmployee);
        txtSearchEmployee.getDocument().addDocumentListener(new DocumentListener() {
            private void filterEmployeeTable() {
                String searchText = txtSearchEmployee.getText().trim();
                if (searchText.isEmpty()) {
                    employeeSorter.setRowFilter(null);
                } else {
                    employeeSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(searchText), 0, 1, 2));
                }
            }
            @Override public void insertUpdate(DocumentEvent e) { filterEmployeeTable(); }
            @Override public void removeUpdate(DocumentEvent e) { filterEmployeeTable(); }
            @Override public void changedUpdate(DocumentEvent e) { filterEmployeeTable(); }
        });

        JScrollPane scrollPane = new JScrollPane(empTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Employee Master List"));

        panel.add(westContainer, BorderLayout.WEST);
        JPanel tableContainer = new JPanel(new BorderLayout(5, 5));
        tableContainer.add(searchPanel, BorderLayout.NORTH);
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        panel.add(tableContainer, BorderLayout.CENTER);

        btnAdd.addActionListener(e -> addEmployee());
        btnRemove.addActionListener(e -> removeEmployee());

        return panel;

    }

    private JPanel createPayrollPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Compute Attendance, Letters & Payroll Formula"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Select Employee:"), gbc);
        comboEmployeeSelect = new JComboBox<>();
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(comboEmployeeSelect, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Position:"), gbc);
        txtPosition = new JTextField();
        txtPosition.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(txtPosition, gbc);

        comboEmployeeSelect.addActionListener(e -> updateSelectedEmployeePosition());

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Overtime Hours Worked:"), gbc);
        txtOvertime = new JTextField("0");
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(txtOvertime, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Excuse Letter Status:"), gbc);
        chkLetterSubmitted = new JCheckBox("Excuse Letter Submitted (Waives 5% penalty)");
        chkLetterSubmitted.setSelected(true);
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(chkLetterSubmitted, gbc);

        JButton btnCompute = new JButton("Compute & Generate Payslip");
        gbc.gridx = 1;
        gbc.gridy = 4;
        panel.add(btnCompute, gbc);

        btnCompute.addActionListener(e -> computePayroll());
        return panel;

    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        payrollReceiptList = new JPanel();
        payrollReceiptList.setLayout(new BoxLayout(payrollReceiptList, BoxLayout.Y_AXIS));
        payrollReceiptList.setBackground(new Color(235, 235, 235));
        JScrollPane scrollPane = new JScrollPane(payrollReceiptList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Payroll Receipts"));

        panel.add(scrollPane, BorderLayout.CENTER);
        JButton btnExport = new JButton("Export Payroll Summary");
        btnExport.addActionListener(e -> SavePayrollToTextFileGUI());
        panel.add(btnExport, BorderLayout.SOUTH);
        return panel;

    }
// receipt instead of table row kay wala lang
    private void addPayrollReceipt(PayrollRecord record) { 
        Object[] rowData = record.toRowData();
        String receiptText = String.format(
                "             PAYROLL RECEIPT%n" +
                        "--------------------------------%n" +
                        "Employee ID:      %s%n" +
                        "Employee Name:    %s%n" +
                        "--------------------------------%n" +
                        "Basic Salary:     %s%n" +
                        "Job Deduction:    %s%n" +
                        "Letter Status:    %s%n" +
                        "Letter Penalty:   %s%n" +
                        "Overtime Pay:     %s%n" +
                        "--------------------------------%n" +
                        "NET PAY:          %s%n",
                rowData[0], rowData[1], rowData[2], rowData[3],
                rowData[4], rowData[5], rowData[6], rowData[7]);

        JTextArea receipt = new JTextArea(receiptText, 12, 34);
        receipt.setEditable(false);
        receipt.setFocusable(false);
        receipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        receipt.setBackground(Color.WHITE);
        receipt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JPanel receiptContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        receiptContainer.setBackground(payrollReceiptList.getBackground());
        receiptContainer.add(receipt);
        payrollReceiptList.add(receiptContainer);
        payrollReceiptList.revalidate();
        payrollReceiptList.repaint();
    }

    private void loadEmployees() {
        if (!EMPLOYEE_FILE.isFile()) {
            return;
        }
        try {
            for (String line : Files.readAllLines(EMPLOYEE_FILE.toPath(), StandardCharsets.UTF_8)) {
                String[] data = line.split("\\s*\\|\\s*", -1);
                if (data.length != 4) {
                    throw new IOException("Invalid employee record: " + line);
                }
                employeeList.add(new Employee(data[1], data[0], data[2], Double.parseDouble(data[3])));
            }
        } catch (IOException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error reading employee file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPayrollData() {
        if (!PAYROLL_FILE.isFile()) {
            return;
        }
        try {
            for (String line : Files.readAllLines(PAYROLL_FILE.toPath(), StandardCharsets.UTF_8)) {
                String[] data = line.split("\\s*\\|\\s*", -1);
                if (data.length != 8) {
                    throw new IOException("Invalid payroll record: " + line);
                }
                boolean letterSubmitted = data[4].contains("(Waived)");
                PayrollRecord record = new PayrollRecord(data[1], data[0], parseAmount(data[2]),
                        parseAmount(data[3]), parseAmount(data[7]), letterSubmitted,
                        parseAmount(data[5]), parseAmount(data[6]));
                payrollRecords.add(record);
                addPayrollReceipt(record);
            }
        } catch (IOException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error reading payroll file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double parseAmount(String amount) {
        return Double.parseDouble(amount.trim().replace("P", "").replace("+", ""));
    }

    private void saveEmployees() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Employee employee : employeeList) {
            lines.add(String.format("%s | %s | %s | %.2f", employee.getId(), employee.getName(),
                    employee.getJobType(), employee.getBasicSalary()));
        }
        writeAtomically(EMPLOYEE_FILE, lines);
    }

    private void writeAtomically(File file, List<String> lines) throws IOException {
        Path target = file.toPath().toAbsolutePath();
        Path temporaryFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private void SavePayrollToTextFileGUI() {
        if (payrollRecords.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No payroll records available to export.", "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;

        }
        try {
            List<String> lines = new ArrayList<>();
            for (PayrollRecord payrollRecord : payrollRecords) {
                Object[] rowData = payrollRecord.toRowData();
                lines.add(String.format("%s | %s | %s | %s | %s | %s | %s | %s",
                        rowData[0], rowData[1], rowData[2], rowData[3],
                        rowData[4], rowData[5], rowData[6], rowData[7]));
            }
            writeAtomically(PAYROLL_FILE, lines);
            JOptionPane.showMessageDialog(this, "Payroll summary exported successfully to "
                    + PAYROLL_FILE.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error exporting payroll summary: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);

        }

    }

    private void addEmployee() {
        String id = txtId.getText().trim();
        String name = txtName.getText().trim();
        String jobType = (String) comboJobType.getSelectedItem();
        String salaryStr = txtSalary.getText().trim();

        if (id.isEmpty() || name.isEmpty() || salaryStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double basicSalary;
        try {
            basicSalary = Double.parseDouble(salaryStr);
            if (basicSalary < 0) {
                throw new NumberFormatException("Negative salary");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive number for salary.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check for duplicate ID
        for (Employee emp : employeeList) {
            if (emp.getId().equals(id)) {
                JOptionPane.showMessageDialog(this, "Employee ID already exists. Please use a unique ID.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Employee newEmployee = new Employee(name, id, jobType, basicSalary);
        employeeList.add(newEmployee);
        try {
            saveEmployees();
        } catch (IOException ex) {
            employeeList.remove(newEmployee);
            JOptionPane.showMessageDialog(this, "Error saving employee file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshEmployeeDropdown();
        refreshEmployeeTable();
        JOptionPane.showMessageDialog(this, "Employee added successfully.");

    }

    private void removeEmployee() {
        int selectedRow = empTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee row to delete.", "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Convert the visible sorted/filtered row index to the original table-model row index.
        int modelRow = empTable.convertRowIndexToModel(selectedRow);
        String targetId = empTableModel.getValueAt(modelRow, 0).toString();
        int employeeIndex = -1;
        for (int i = 0; i < employeeList.size(); i++) {
            if (employeeList.get(i).getId().equals(targetId)) {
                employeeIndex = i;
                break;
            }
        }
        if (employeeIndex < 0) {
            return;
        }
        Employee removedEmployee = employeeList.remove(employeeIndex);
        try {
            saveEmployees();
        } catch (IOException ex) {
            employeeList.add(employeeIndex, removedEmployee);
            JOptionPane.showMessageDialog(this, "Error saving employee file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshEmployeeTable();
        refreshEmployeeDropdown();
        JOptionPane.showMessageDialog(this, "Employee removed successfully.");

    }

    private void computePayroll() {
        if (employeeList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No employees available to compute payroll.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;

        }
        String selectedSelection = (String) comboEmployeeSelect.getSelectedItem();
        if (selectedSelection == null)
            return;

        String empId = selectedSelection.split(" - ")[0];
        Employee target = null;
        for (Employee e : employeeList) {
            if (e.getId().equalsIgnoreCase(empId)) {
                target = e;
                break;
            }
        }
        if (target == null)
            return;
        try {
            double otHours = Double.parseDouble(txtOvertime.getText().trim());
            boolean letterSubmitted = chkLetterSubmitted.isSelected();

            // Payroll Calculation Logic
            double basic = target.getBasicSalary();
            double jobPercentage = jobDeductionRules.get(target.getJobType());
            double jobDeduction = basic * jobPercentage;

            double letterDeduction = 0.0;// Initialize letter deduction
            if (!letterSubmitted) {
                letterDeduction = basic * 0.05; // 5% penalty

            }
            // Overtime Pay Calculation: 1.25x the hourly rate for each hour worked beyond 8
            // hours/day (assuming 28 working days in a month)
            double hourlyRate = (basic / 28.0) / 8.0;
            double overtimePay = otHours * (hourlyRate * 1.25);

            // Net Pay Calculation
            double totalDeductions = jobDeduction + letterDeduction;
            double netPay = (basic + overtimePay) - totalDeductions;

            PayrollRecord record = new PayrollRecord(target.getName(), target.getId(), basic, jobDeduction, netPay,
                    letterSubmitted, letterDeduction, overtimePay);
            payrollRecords.add(record);
            addPayrollReceipt(record);

            JOptionPane.showMessageDialog(this, String.format("Payroll computed for %s (ID: %s)\nNet Pay: P%.2f",
                    target.getName(), target.getId(), netPay), "Payroll Computed", JOptionPane.INFORMATION_MESSAGE);
            JOptionPane.showMessageDialog(this, "Payslip generated.", "Payslip Generated",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for overtime hours.", "Error",
                    JOptionPane.ERROR_MESSAGE);

        }

    }

    private void refreshEmployeeDropdown() {
        comboEmployeeSelect.removeAllItems();
        for (Employee emp : employeeList) {
            comboEmployeeSelect.addItem(emp.getId() + " - " + emp.getName());
        }
        updateSelectedEmployeePosition();
    }

    private void updateSelectedEmployeePosition() {
        if (txtPosition == null) {
            return;
        }
        String selectedSelection = (String) comboEmployeeSelect.getSelectedItem();
        if (selectedSelection == null) {
            txtPosition.setText("");
            return;
        }
        String empId = selectedSelection.split(" - ")[0];
        for (Employee employee : employeeList) {
            if (employee.getId().equalsIgnoreCase(empId)) {
                txtPosition.setText(employee.getJobType());
                return;
            }
        }
        txtPosition.setText("");
    }

    private void refreshEmployeeTable() {
        empTableModel.setRowCount(0); // Clear existing rows
        for (Employee emp : employeeList) {
            empTableModel.addRow(new Object[] { emp.getId(), emp.getName(), emp.getJobType(),
                    String.format("P%.2f", emp.getBasicSalary()) });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EmployeeManagementGUI gui = new EmployeeManagementGUI();
            gui.setVisible(true);
        });
    }

}
