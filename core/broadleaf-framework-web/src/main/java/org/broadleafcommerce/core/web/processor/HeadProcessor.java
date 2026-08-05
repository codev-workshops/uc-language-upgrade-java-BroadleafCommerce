/*
 * #%L
 * BroadleafCommerce Framework Web
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
package org.broadleafcommerce.core.web.processor;

import org.broadleafcommerce.core.web.processor.extension.HeadProcessorExtensionListener;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import jakarta.annotation.Resource;

/**
 * A Thymeleaf processor that will include the standard head element. It will also set the
 * following variables for use by the head fragment.
 * 
 * <ul>
 *  <li><b>pageTitle</b> - The title of the page</li>
 *  <li><b>additionalCss</b> - An additional, page specific CSS file to include</li>
 *  <li><b>metaDescription</b> - Optional, Content for the Meta-Description tag</li>
 *  <li><b>metaKeywords</b> - Optional, Content for the Meta-Keywords tag</li>
 *  <li><b>metaRobot</b> - Optional, Content for the Meta-Robots tag</li>
 * </ul>
 * 
 * @author apazzolini
 *
 * @deprecated
 *
 * The entire FragmentAndTarget class has been deprecated in favor of a completely new system in Thymeleaf 2.1
 * The referenced issue can be found at https://github.com/thymeleaf/thymeleaf/issues/205
 *
 * Use th:include or th:replace within the head tag and include the variables to replicate the behaviour.
 *
 * Examples:
 *
 * <head th:include="/layout/partials/head (pageTitle='My Page Title')"></head>
 * <head th:include="/layout/partials/head (twovar=${value2},onevar=${value1})">...</head>
 *
 */
@Deprecated
public class HeadProcessor extends AbstractElementTagProcessor {

    @Resource(name = "blHeadProcessorExtensionManager")
    protected HeadProcessorExtensionListener extensionManager;

    protected String HEAD_PARTIAL_PATH = "layout/partials/head";
    
    /**
     * Sets the name of this processor to be used in Thymeleaf template
     */
    public HeadProcessor() {
        super(TemplateMode.HTML, "blc", "head", true, null, false, 10000);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
                             IElementTagStructureHandler structureHandler) {
        // The pageTitle attribute could be an expression that needs to be evaluated. Try to evaluate, but fall back
        // to its text value if the expression wasn't able to be processed. This will allow things like
        // pageTitle="Hello this is a string"
        // as well as expressions like
        // pageTitle="${'Hello this is a ' + product.name}"

        String pageTitle = tag.getAttributeValue("pageTitle");
        try {
            IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
            IStandardExpression expression = parser.parseExpression(context, pageTitle);
            pageTitle = (String) expression.execute(context);
        } catch (TemplateProcessingException e) {
            // Do nothing.
        }
        structureHandler.setLocalVariable("pageTitle", pageTitle);
        structureHandler.setLocalVariable("additionalCss", tag.getAttributeValue("additionalCss"));

        extensionManager.processAttributeValues(context, tag, structureHandler);

        structureHandler.replaceWith("<div th:replace=\"" + HEAD_PARTIAL_PATH + "\"></div>", true);
    }

}
