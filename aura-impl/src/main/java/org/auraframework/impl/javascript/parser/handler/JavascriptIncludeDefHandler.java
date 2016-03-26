/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.auraframework.impl.javascript.parser.handler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.auraframework.def.DefDescriptor;
import org.auraframework.def.IncludeDef;
import org.auraframework.expression.PropertyReference;
import org.auraframework.impl.root.library.IncludeDefImpl.Builder;
import org.auraframework.impl.util.TextTokenizer;
import org.auraframework.system.Source;
import org.auraframework.throwable.AuraRuntimeException;
import org.auraframework.throwable.quickfix.InvalidExpressionException;
import org.auraframework.throwable.quickfix.QuickFixException;

public class JavascriptIncludeDefHandler extends JavascriptHandler<IncludeDef, IncludeDef> {

    private final Builder builder = new Builder();

    public JavascriptIncludeDefHandler(DefDescriptor<IncludeDef> descriptor, Source<?> source) {
        super(descriptor, source);
    }

    @Override
    public IncludeDef getDefinition() {
        try {
            String code = source.getContents();
            String filename = source.getUrl();
            
            // Ignore the results for now
            getCompressedSource(code, filename);

            try {
                TextTokenizer tt = TextTokenizer.tokenize(code, getLocation());
                tt.addExpressionRefs(this);
            } catch (InvalidExpressionException iee) {
                // completely ignore we don't know what is in libraries, and until the TextTokenizer is smarter,
                // we'll just bomb out here.
            }

            builder.setCode(code);
            setDefBuilderFields(builder);

        } catch (QuickFixException qfe) {
            return createDefinition(qfe);
        } catch (IOException e) {
            return createDefinition(new AuraRuntimeException(e, getLocation()));
        }

        return builder.build();
    }

    @Override
    public void addExpressionReferences(Set<PropertyReference> propRefs) {
        builder.addExpressionReferences(propRefs);
    }

    @Override
    protected IncludeDef createDefinition(Throwable error) {
        setDefBuilderFields(builder);
        builder.setParseError(error);
        return builder.build();
    }

    @Override
    protected IncludeDef createDefinition(Map<String, Object> map) throws QuickFixException {
        // work done in getDefinition instead
        return null;
    }
}
