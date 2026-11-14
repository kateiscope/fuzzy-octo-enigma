class Employee {
    private String name;
    private String id;
    private String jobType;
    private double basicSalary;

    public Employee(String name, String id, String jobType, double basicSalary) {
        this.name = name;
        this.id = id;
        this.jobType = jobType;
        this.basicSalary = basicSalary;

    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getJobType() {
        return jobType;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

}
