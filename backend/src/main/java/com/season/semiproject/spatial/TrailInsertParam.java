package com.season.semiproject.spatial;

/** MyBatis parameter/generated-key holder for one `trail` row (mirrors PathInfoVO's pattern:
 *  the same object is passed in as the insert parameter and receives the generated id back). */
public class TrailInsertParam {

    private Long id;
    private Integer courseId;
    private String sourceFile;
    private String sourceMountainName;
    private String sourceCourseName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSourceMountainName() {
        return sourceMountainName;
    }

    public void setSourceMountainName(String sourceMountainName) {
        this.sourceMountainName = sourceMountainName;
    }

    public String getSourceCourseName() {
        return sourceCourseName;
    }

    public void setSourceCourseName(String sourceCourseName) {
        this.sourceCourseName = sourceCourseName;
    }
}
