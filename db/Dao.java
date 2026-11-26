package com.mycompany.snakegame.db;

import com.mycompany.snakegame.exception.DatabaseException;
import java.util.List;

public interface Dao<T, K> {

    void insert(T item) throws DatabaseException;      

    List<T> findAll() throws DatabaseException;        

    boolean deleteById(K id) throws DatabaseException; 
}
