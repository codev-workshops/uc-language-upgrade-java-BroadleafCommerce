/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2013 Broadleaf Commerce
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
package org.broadleafcommerce.common.util.sql.importsql;

import org.broadleafcommerce.common.logging.SupportLogManager;
import org.broadleafcommerce.common.logging.SupportLogger;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.internal.script.SingleLineSqlScriptExtractor;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a utility class that is only meant to be used for testing the BLC demo on SQL Server. In our current
 * import sql files, there are a number of value declarations that are incompatible with Sql Server. This
 * custom extractor takes care of transforming those values into something SQL Server understands.
 *
 * @author Jeff Fischer
 */
public class DemoSqlServerSingleLineSqlCommandExtractor extends SingleLineSqlScriptExtractor {

    private static final long serialVersionUID = 1L;

    private static final SupportLogger LOGGER = SupportLogManager.getLogger("UserOverride", DemoSqlServerSingleLineSqlCommandExtractor.class);

    private static final String BOOLEANTRUEMATCH = "(?i)(true)";
    private static final String BOOLEANFALSEMATCH = "(?i)(false)";
    private static final String TIMESTAMPMATCH = "(?i)(current_date)";
    public static final String TRUE = "'TRUE'";
    public static final String FALSE = "'FALSE'";
    public static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

    protected boolean alreadyRun = false;

    @Override
    public List<String> extractCommands(Reader reader, Dialect dialect) {
        if (!alreadyRun) {
            alreadyRun = true;
            LOGGER.support("Converting hibernate.hbm2ddl.import_files sql statements for compatibility with SQL Server");
        }

        List<String> statements = new ArrayList<>(super.extractCommands(reader, dialect));
        handleBooleans(statements);

        return statements;
    }

    protected void handleBooleans(List<String> statements) {
        for (int j = 0; j < statements.size(); j++) {
            //try start matches
            statements.set(j, statements.get(j).replaceAll(BOOLEANTRUEMATCH + "\\s*[,]", TRUE + ","));
            statements.set(j, statements.get(j).replaceAll(BOOLEANFALSEMATCH + "\\s*[,]", FALSE + ","));
            statements.set(j, statements.get(j).replaceAll(TIMESTAMPMATCH + "\\s*[,]", CURRENT_TIMESTAMP + ","));

            //try middle matches
            statements.set(j, statements.get(j).replaceAll("[,]\\s*" + BOOLEANTRUEMATCH + "\\s*[,]", "," + TRUE + ","));
            statements.set(j, statements.get(j).replaceAll("[,]\\s*" + BOOLEANFALSEMATCH + "\\s*[,]", "," + FALSE + ","));
            statements.set(j, statements.get(j).replaceAll("[,]\\s*" + TIMESTAMPMATCH + "\\s*[,]", "," + CURRENT_TIMESTAMP + ","));

            //try end matches
            statements.set(j, statements.get(j).replaceAll("[,]\\s*" + BOOLEANTRUEMATCH, "," + TRUE));
            statements.set(j, statements.get(j).replaceAll("[,]\\s*" + BOOLEANFALSEMATCH, "," + FALSE));
            statements.set(j, statements.get(j).replaceAll("[,]\\s*" + TIMESTAMPMATCH, "," + CURRENT_TIMESTAMP));

            //try matches for updates
            statements.set(j, statements.get(j).replaceAll("[=]\\s*" + BOOLEANTRUEMATCH, "=" + TRUE));
            statements.set(j, statements.get(j).replaceAll("[=]\\s*" + BOOLEANFALSEMATCH, "=" + FALSE));
        }
    }
}
