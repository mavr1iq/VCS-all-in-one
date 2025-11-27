package vcs.repository.classes;

import java.util.List;

public class MergeResults {
    private final boolean success;
    private final List<FileConflict> conflicts;

    public MergeResults(boolean success, List<FileConflict> conflicts) {
        this.success = success;
        this.conflicts = conflicts;
    }

    public boolean isSuccess() { return success; }
    public List<FileConflict> getConflicts() { return conflicts; }
}
