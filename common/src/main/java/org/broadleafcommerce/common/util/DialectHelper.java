/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2014 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.broadleafcommerce.common.util;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author Jeff Fischer
 */
@Repository("blDialectHelper")
public class DialectHelper {

    @PersistenceContext(unitName="blPU")
    protected EntityManager em;

    protected Dialect cachedDialect = null;

    public synchronized Dialect getHibernateDialect() {
        if (cachedDialect == null) {
            SessionFactoryImplementor factory = (SessionFactoryImplementor) em.unwrap(Session.class).getSessionFactory();
            cachedDialect = factory.getJdbcServices().getDialect();
        }
        return cachedDialect;
    }

    public boolean isOracle() {
        //Since should handle other Oracle dialects as well, since they derive from OracleDialect
        return getHibernateDialect() instanceof OracleDialect;
    }

    public boolean isPostgreSql() {
        //Since should handle other Postgres dialects as well, since they derive from PostgreSQLDialect
        return getHibernateDialect() instanceof PostgreSQLDialect;
    }

    public boolean isSqlServer() {
        return getHibernateDialect() instanceof SQLServerDialect;
    }

    public boolean isMySql() {
        return getHibernateDialect() instanceof MySQLDialect;
    }

    public void clear() {
        cachedDialect = null;
    }
}
