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
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;


/**
 * Works with the blc:bundle tag.   
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
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
        String name = tag.getAttributeValue("name");
        String mappingPrefix = tag.getAttributeValue("mapping-prefix");
        boolean async = tag.hasAttribute("async");
        boolean defer = tag.hasAttribute("defer");
        IModelFactory modelFactory = context.getModelFactory();
        IModel model = modelFactory.createModel();
        
        List<String> files = new ArrayList<String>();
        for (String file : tag.getAttributeValue("files").split(",")) {
            files.add(file.trim());
        }
        List<String> additionalBundleFiles = bundlingService.getAdditionalBundleFiles(name);
        if (additionalBundleFiles != null) {
            files.addAll(additionalBundleFiles);
        }
        
        if (getBundleEnabled()) {
            String bundleResourceName = bundlingService.resolveBundleResourceName(name, mappingPrefix, files);
            String bundleUrl = getBundleUrl(context, bundleResourceName);
            addElement(model, modelFactory, bundleUrl, async, defer);
        } else {
            IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
            for (String file : files) {
                file = file.trim();
                IStandardExpression expression = parser.parseExpression(context, "@{'" + mappingPrefix + file + "'}");
                String value = (String) expression.execute(context);
                addElement(model, modelFactory, value, async, defer);
            }
        }
        
        structureHandler.replaceWith(model, false);
    }
    
    protected String getBundleUrl(ITemplateContext context, String bundleName) {
        String bundleUrl = bundleName;

        if (!StringUtils.startsWith(bundleUrl, "/")) {
            bundleUrl = "/" + bundleUrl;
        }

        if (context instanceof IWebContext) {
            String contextPath = ((IWebContext) context).getExchange().getRequest().getApplicationPath();

            if (StringUtils.isNotEmpty(contextPath)) {
                bundleUrl = contextPath + bundleUrl;
            }
        }

        return bundleUrl;
    }

    protected void addElement(IModel model, IModelFactory modelFactory, String src, boolean async, boolean defer) {
        if (src.contains(";")) {
            src = src.substring(0, src.indexOf(';'));
        }
        
        if (src.endsWith(".js")) {
            addScriptElement(model, modelFactory, src, async, defer);
        } else if (src.endsWith(".css")) {
            addLinkElement(model, modelFactory, src);
        } else {
            throw new IllegalArgumentException("Unknown extension for: " + src + " - only .js and .css are supported");
        }
    }

    protected void addScriptElement(IModel model, IModelFactory modelFactory, String src, boolean async, boolean defer) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("type", "text/javascript");
        attrs.put("src", src);
        if (async) {
            attrs.put("async", "async");
        }
        if (defer) {
            attrs.put("defer", "defer");
        }
        model.add(modelFactory.createOpenElementTag("script", attrs, null, false));
        model.add(modelFactory.createCloseElementTag("script"));
    }
    
    protected void addLinkElement(IModel model, IModelFactory modelFactory, String src) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("rel", "stylesheet");
        attrs.put("href", src);
        model.add(modelFactory.createStandaloneElementTag("link", attrs, null, false, false));
    }
}
