/*
 * #%L
 * BroadleafCommerce Open Admin Platform
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
package org.broadleafcommerce.openadmin.web.processor;

import org.broadleafcommerce.common.web.dialect.AbstractModelVariableModifierProcessor;
import org.broadleafcommerce.openadmin.server.security.domain.AdminMenu;
import org.broadleafcommerce.openadmin.server.security.domain.AdminUser;
import org.broadleafcommerce.openadmin.server.security.service.AdminSecurityService;
import org.broadleafcommerce.openadmin.server.security.service.navigation.AdminNavigationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;

import jakarta.annotation.Resource;

/**
 * A Thymeleaf processor that will add the appropriate AdminModules to the model. It does this by
 * iterating through the permissions specified in the SecurityContexts AdminUser object and adding the
 * appropriate section to the model attribute specified by resultVar
 *
 * This is useful in constructing the left navigation menu for the admin console.
 *
 * @author elbertbautista
 */
@Component("blAdminModuleProcessor")
public class AdminModuleProcessor extends AbstractModelVariableModifierProcessor {

    private static final String ANONYMOUS_USER_NAME = "anonymousUser";

    @Resource(name = "blAdminNavigationService")
    protected AdminNavigationService adminNavigationService;
    
    @Resource(name = "blAdminSecurityService")
    protected AdminSecurityService securityService;

    /**
     * Sets the name of this processor to be used in Thymeleaf template
     */
    public AdminModuleProcessor() {
        super("blc_admin", "admin_module", 10001);
    }

    @Override
    protected void modifyModelAttributes(ITemplateContext context, IProcessableElementTag tag,
                                         IElementTagStructureHandler structureHandler) {
        String resultVar = tag.getAttributeValue("resultVar");

        AdminUser user = getPersistentAdminUser();
        if (user != null) {
            AdminMenu menu = adminNavigationService.buildMenu(user);
            addToModel(structureHandler, resultVar, menu);
        }
    }

    protected AdminUser getPersistentAdminUser() {
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx != null) {
            Authentication auth = ctx.getAuthentication();
            if (auth != null && !auth.getName().equals(ANONYMOUS_USER_NAME)) {
                UserDetails temp = (UserDetails) auth.getPrincipal();

                return securityService.readAdminUserByUserName(temp.getUsername());
            }
        }

        return null;
    }
}
