package vcs.repository.dao;

import java.util.List;

public interface GeneralDao<T> {
    T getById(int id);
    List<T> getAll();
    void add(T t);
    void update(T t);
    void delete(T t);
}
