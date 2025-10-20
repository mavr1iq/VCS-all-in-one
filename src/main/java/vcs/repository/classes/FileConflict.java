package vcs.repository.classes;

public class FileConflict {
    private final String filePath;
    private final String conflictDetails;

    public FileConflict(String filePath, String conflictDetails) {
        this.filePath = filePath;
        this.conflictDetails = conflictDetails;
    }

    public String getFilePath() { return filePath; }
    public String getConflictDetails() { return conflictDetails; }
}
