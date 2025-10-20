package vcs.repository.factories;

import vcs.repository.VcsType;
import vcs.repository.adapters.GitAdapter;
import vcs.repository.adapters.MercurialAdapter;
import vcs.repository.adapters.SvnAdapter;
import vcs.repository.adapters.VcsAdapter;

public class AdapterFactory {

    public VcsAdapter getAdapter(VcsType type) {
        if (type == null) {
            throw new IllegalArgumentException("VCS type cannot be null");
        }

        return switch (type) {
            case GIT -> new GitAdapter();
            case SVN -> new SvnAdapter();
            case MERCURIAL -> new MercurialAdapter();
            default -> throw new IllegalArgumentException("Unknown VCS type: " + type);
        };
    }
}