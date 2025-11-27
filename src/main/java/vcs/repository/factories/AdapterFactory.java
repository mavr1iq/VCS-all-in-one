package vcs.repository.factories;

import vcs.repository.VcsType;
import vcs.repository.adapters.GitAdapter;
import vcs.repository.adapters.MercurialAdapter;
import vcs.repository.adapters.SvnAdapter;
import vcs.repository.adapters.VcsAdapter;
import vcs.repository.dao.db.DatabaseContext;

public class AdapterFactory {
    DaoFactory daoFactory;

    public AdapterFactory(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    public VcsAdapter getAdapter(VcsType type) {
        if (type == null) {
            throw new IllegalArgumentException("VCS type cannot be null");
        }

        return switch (type) {
            case GIT -> new GitAdapter(daoFactory);
            case SVN -> new SvnAdapter(daoFactory);
            case MERCURIAL -> new MercurialAdapter(daoFactory);
            default -> throw new IllegalArgumentException("Unknown VCS type: " + type);
        };
    }
}