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
package org.broadleafcommerce.common.web.processor;

import org.apache.commons.lang3.StringUtils;
import org.broadleafcommerce.common.resource.service.ResourceBundlingService;
import org.broadleafcommerce.common.util.BLCSystemProperty;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;


/**
 * <p>
 * Works with the blc:bundle tag.   See the class-level documentation in the original Broadleaf source for the full set
 * of supported attributes and example output.   Migrated to the Thymeleaf 3 {@link AbstractElementTagProcessor} API:
 * Thymeleaf 3 removed the {@code org.thymeleaf.dom.*} model, so the generated SCRIPT/LINK markup is now produced as a
 * string and emitted through {@link IElementTagStructureHandler#replaceWith(CharSequence, boolean)}.
 *
 * @author apazzolini
 * @author bpolster
 * @see {@link ResourceBundlingService}
 */
public class ResourceBundleProcessor extends AbstractElementTagProcessor {

    @Resource(name = "blResourceBundlingService")
    protected ResourceBundlingService bundlingService;

    protected boolean getBundleEnabled() {
        return BLCSystemProperty.resolveBooleanSystemProperty("bundle.enabled");
    }

    public ResourceBundleProcessor() {
        super(TemplateMode.HTML, "blc", "bundle", true, null, false, 10000);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
            IElementTagStructureHandler structureHandler) {
        String name = tag.getAttributeValue("name");
        String mappingPrefix = tag.getAttributeValue("mapping-prefix");
        boolean async = tag.hasAttribute("async");
        boolean defer = tag.hasAttribute("defer");
        List<String> files = new ArrayList<String>();
        for (String file : tag.getAttributeValue("files").split(",")) {
            files.add(file.trim());
        }
        List<String> additionalBundleFiles = bundlingService.getAdditionalBundleFiles(name);
        if (additionalBundleFiles != null) {
            files.addAll(additionalBundleFiles);
        }

        StringBuilder markup = new StringBuilder();
        if (getBundleEnabled()) {
            String bundleResourceName = bundlingService.resolveBundleResourceName(name, mappingPrefix, files);
            String bundleUrl = getBundleUrl(bundleResourceName);
            markup.append(getElementMarkup(bundleUrl, async, defer));
        } else {
            IEngineConfiguration configuration = context.getConfiguration();
            IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);
            for (String file : files) {
                file = file.trim();
                IStandardExpression expression = parser.parseExpression(context, "@{'" + mappingPrefix + file + "'}");
                String value = (String) expression.execute(context);
                markup.append(getElementMarkup(value, async, defer));
            }
        }

        structureHandler.replaceWith(markup.toString(), false);
    }

    /**
     * Adds the context path to the bundleUrl.    We don't use the Thymeleaf "@" syntax or any other mechanism to
     * encode this URL as the resolvers could have a conflict.
     *
     * For example, resolving a bundle named "style.css" that has a file also named "style.css" creates problems as
     * the TF or version resolvers both want to version this file.
     */
    protected String getBundleUrl(String bundleName) {
        String bundleUrl = bundleName;

        if (!StringUtils.startsWith(bundleUrl, "/")) {
            bundleUrl = "/" + bundleUrl;
        }

        BroadleafRequestContext brc = BroadleafRequestContext.getBroadleafRequestContext();
        if (brc != null && brc.getRequest() != null) {
            String contextPath = brc.getRequest().getContextPath();
            if (StringUtils.isNotEmpty(contextPath)) {
                bundleUrl = contextPath + bundleUrl;
            }
        }

        return bundleUrl;
    }

    protected String getScriptMarkup(String src, boolean async, boolean defer) {
        StringBuilder sb = new StringBuilder("<script type=\"text/javascript\" src=\"").append(src).append("\"");
        if (async) {
            sb.append(" async");
        }
        if (defer) {
            sb.append(" defer");
        }
        sb.append("></script>");
        return sb.toString();
    }

    protected String getLinkMarkup(String src) {
        return "<link rel=\"stylesheet\" href=\"" + src + "\" />";
    }

    protected String getElementMarkup(String src, boolean async, boolean defer) {
        if (src.contains(";")) {
            src = src.substring(0, src.indexOf(';'));
        }

        if (src.endsWith(".js")) {
            return getScriptMarkup(src, async, defer);
        } else if (src.endsWith(".css")) {
            return getLinkMarkup(src);
        } else {
            throw new IllegalArgumentException("Unknown extension for: " + src + " - only .js and .css are supported");
        }
    }
}
