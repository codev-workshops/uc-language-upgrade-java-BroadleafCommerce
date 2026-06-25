/*
 * #%L
 * BroadleafCommerce Integration
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
package org.broadleafcommerce.test;

/**
 * Local replacement for the removed Spring {@code TransactionConfigurationAttributes} class.
 * Holds transaction configuration attributes for test execution.
 */
public class TransactionConfigurationAttributes {

    private final String transactionManagerName;
    private final boolean defaultRollback;

    public TransactionConfigurationAttributes(String transactionManagerName, boolean defaultRollback) {
        this.transactionManagerName = transactionManagerName;
        this.defaultRollback = defaultRollback;
    }

    public String getTransactionManagerName() {
        return this.transactionManagerName;
    }

    public boolean isDefaultRollback() {
        return this.defaultRollback;
    }

    @Override
    public String toString() {
        return "TransactionConfigurationAttributes[transactionManagerName='" + transactionManagerName +
                "', defaultRollback=" + defaultRollback + "]";
    }
}
