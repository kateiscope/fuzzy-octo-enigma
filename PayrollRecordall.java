import java.math.BigDecimal;
import java.math.RoundingMode;

class PayrollRecord {
    private String employeeName;
    private String employeeId;
    private double basicSalary;
    private double jobDeduction;
    private double netPay;
    private boolean letterSubmitted;
    private double letterDeduction;
    private double overtimePay;

    public PayrollRecord(String employeeName, String employeeId, double basicSalary, double jobDeduction, double netPay,
            boolean letterSubmitted, double letterDeduction, double overtimePay) {
        this.employeeName = employeeName;
        this.employeeId = employeeId;
        this.basicSalary = basicSalary;
        this.jobDeduction = jobDeduction;
        this.netPay = netPay;
        this.letterSubmitted = letterSubmitted;
        this.letterDeduction = letterDeduction;
        this.overtimePay = overtimePay;
    }

    public Object[] toRowData() {
        return new Object[] { employeeId, employeeName, "P" + format(basicSalary), "-P" + format(jobDeduction),
                letterSubmitted ? "Submitted(Waived)" : "Missing(-5% Penalty)", "-P" + format(letterDeduction),
                "+P" + format(overtimePay), "P" + format(netPay) };

    }

    private String format(double value) {
        BigDecimal bd = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        return bd.toString();
    }
}
