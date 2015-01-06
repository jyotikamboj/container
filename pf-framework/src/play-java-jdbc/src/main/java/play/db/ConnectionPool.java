/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import javax.sql.DataSource;

import play.Configuration;

/**
 * Connection pool API for managing data sources.
 */
public interface ConnectionPool {

    /**
     * Create a data source with the given configuration.
     *
     * @param name the database name
     * @param configuration the data source configuration
     * @param classLoader the database class loader
     * @return a data source backed by a connection pool
     */
    public DataSource create(String name, Configuration configuration, ClassLoader classLoader);

    /**
     * Close the given data source.
     *
     * @param dataSource the data source to close
     */
    public void close(DataSource dataSource);

}
