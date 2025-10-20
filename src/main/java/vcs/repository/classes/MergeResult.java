package vcs.repository.classes;

import java.util.List;

public class MergeResult {
    private final boolean success;
    private final List<FileConflict> conflicts;

    public MergeResult(boolean success, List<FileConflict> conflicts) {
        this.success = success;
        this.conflicts = conflicts;
    }

    public boolean isSuccess() { return success; }
    public List<FileConflict> getConflicts() { return conflicts; }
}
