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
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IStandaloneElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;


/**
 * <p>
 * Works with the blc:bundle tag.   
 * 
 * <p>
 * This processor does not do the actual bundling.   It merely changes the URL which causes the 
 * other bundling components to be invoked through the normal static resource handling processes.
 * 
 * <p>
 * This processor relies {@code bundle.enabled}.   If this property is false (typical for dev) then the list of
 * resources will be output as individual SCRIPT or LINK elements for each JavaScript or CSS file respectively.
 * 
 * <p>
 * To use this processor, supply a name, mapping prefix, and list of files.   
 * 
 * <pre>
 * {@code
 * <blc:bundle name="lib.js" 
 *             mapping-prefix="/js/"
 *             files="plugins.js,
 *                    libs/jquery.MetaData.js,
 *                    libs/jquery.rating.pack.js,
 *                    libs/jquery.dotdotdot-1.5.1.js" />
 *  }
 * </pre>                  
 * 
 * <p>
 * With bundling enabled this will turn into:
 * 
 * <pre>
 * 
 * {@code
 *  <script type="text/javascript" src="/js/lib-blbundle12345.js" />
 * }
 * </pre>
 * 
 * <p>
 * Where the <b>-blbundle12345</b> is used by the BundleUrlResourceResolver to determine the
 * actual bundle name.  
 * 
 * <p>
 * With bundling disabled this turns into:
 * 
 * <pre>
 * {@code
 *  <script type="text/javascript" src="/js/plugins.js" />
 *  <script type="text/javascript" src="/js/jquery.MetaData.js" />
 *  <script type="text/javascript" src="/js/jquery.rating.pack.js.js" />
 *  <script type="text/javascript" src="/js/jquery.dotdotdot-1.5.1.js" />
 * }
 * </pre>
 * 
 * <p>
 * This processor also supports producing the 'async' and 'defer' attributes for Javascript files. For instance:
 * 
 * <pre>
 * {@code
 * <blc:bundle name="lib.js" 
 *             async="true"
 *             defer="true"
 *             mapping-prefix="/js/"
 *             files="plugins.js,
 *                    libs/jquery.MetaData.js,
 *                    libs/jquery.rating.pack.js,
 *                    libs/jquery.dotdotdot-1.5.1.js" />
 *  }
 * </pre>
 * 
 * <p>
 * If bundling is turned on, the single output file contains the 'async' and 'defer' name-only attributes. When bundling is
 * turned off, then those name-only attributes are applied to each individual file reference.
 * 
 * <p>
 * This processor only supports files that end in <b>.js</b> and <b>.css</b>
 * 
 * @param <b>name</b>           (required) the final name prefix of the bundle
 * @param <b>mapping-prefix</b> (required) the prefix appended to the final tag output whether that be 
 *                              the list of files or the single minified file
 * @param <b>files</b>          (required) a comma-separated list of files that should be bundled together
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
        boolean async = tag.getAttribute("async") != null;
        boolean defer = tag.getAttribute("defer") != null;
        List<String> files = new ArrayList<String>();
        for (String file : tag.getAttributeValue("files").split(",")) {
            files.add(file.trim());
        }
        List<String> additionalBundleFiles = bundlingService.getAdditionalBundleFiles(name);
        if (additionalBundleFiles != null) {
            files.addAll(additionalBundleFiles);
        }

        IModelFactory modelFactory = context.getModelFactory();
        IModel model = modelFactory.createModel();

        if (getBundleEnabled()) {
            String bundleResourceName = bundlingService.resolveBundleResourceName(name, mappingPrefix, files);
            String bundleUrl = getBundleUrl(context, bundleResourceName);
            model.add(getElement(modelFactory, bundleUrl, async, defer));
        } else {
            IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
            for (String file : files) {
                file = file.trim();
                IStandardExpression expression = parser.parseExpression(context, "@{'" + mappingPrefix + file + "'}");
                String value = (String) expression.execute(context);
                model.add(getElement(modelFactory, value, async, defer));
            }
        }

        structureHandler.replaceWith(model, false);
    }
    
    /**
     * Adds the context path to the bundleUrl.    We don't use the Thymeleaf "@" syntax or any other mechanism to 
     * encode this URL as the resolvers could have a conflict.   
     * 
     * For example, resolving a bundle named "style.css" that has a file also named "style.css" creates problems as
     * the TF or version resolvers both want to version this file.
     *  
     * @param arguments
     * @param bundleName
     * @return
     */
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

    protected IStandaloneElementTag getScriptElement(IModelFactory modelFactory, String src, boolean async, boolean defer) {
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("type", "text/javascript");
        attrs.put("src", src);
        if (async) {
            attrs.put("async", "");
        }
        if (defer) {
            attrs.put("defer", "");
        }
        return modelFactory.createStandaloneElementTag("script", attrs, AttributeValueQuotes.DOUBLE, false, true);
    }
    
    protected IStandaloneElementTag getLinkElement(IModelFactory modelFactory, String src) {
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("rel", "stylesheet");
        attrs.put("href", src);
        return modelFactory.createStandaloneElementTag("link", attrs, AttributeValueQuotes.DOUBLE, false, true);
    }
    
    protected IStandaloneElementTag getElement(IModelFactory modelFactory, String src, boolean async, boolean defer) {
        if (src.contains(";")) {
            src = src.substring(0, src.indexOf(';'));
        }
        
        if (src.endsWith(".js")) {
            return getScriptElement(modelFactory, src, async, defer);
        } else if (src.endsWith(".css")) {
            return getLinkElement(modelFactory, src);
        } else {
            throw new IllegalArgumentException("Unknown extension for: " + src + " - only .js and .css are supported");
        }
    }
}
