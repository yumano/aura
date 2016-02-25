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
package org.auraframework.impl;

import java.io.IOException;

import org.auraframework.Aura;
import org.auraframework.def.ComponentDef;
import org.auraframework.def.DefDescriptor;
import org.auraframework.def.Definition;
import org.auraframework.def.ModelDef;
import org.auraframework.def.ValueDef;
import org.auraframework.impl.expression.PropertyReferenceImpl;
import org.auraframework.impl.java.model.JavaModelDefFactory;
import org.auraframework.impl.java.model.JavaModelDefImpl;
import org.auraframework.impl.system.DefDescriptorImpl;
import org.auraframework.instance.Model;
import org.auraframework.system.Annotations;
import org.auraframework.system.Location;
import org.auraframework.test.source.StringSourceLoader;
import org.auraframework.throwable.AuraExecutionException;
import org.auraframework.throwable.NoAccessException;
import org.auraframework.throwable.quickfix.DefinitionNotFoundException;
import org.auraframework.throwable.quickfix.InvalidDefinitionException;
import org.auraframework.util.json.Json;

/**
 * This class provides automation for Java models.
 *
 * @hierarchy Aura.Unit Tests.Components.Model.Java Model
 * @priority high
 */
public class JavaModelTest extends AuraImplTestCase {

    private static final DefDescriptor<ModelDef> descriptor = new DefDescriptor<ModelDef>() {
        private static final long serialVersionUID = -2368424955441005888L;

        @Override
        public void serialize(Json json) throws IOException {
            json.writeValue(getQualifiedName());
        }

        @Override
        public String getPrefix() {
            return "java";
        }

        @Override
        public String getNamespace() {
            return org.auraframework.impl.java.model.TestModel.class.getPackage().getName();
        }

        @Override
        public String getName() {
            return org.auraframework.impl.java.model.TestModel.class.getSimpleName();
        }

        @Override
        public String getQualifiedName() {
            return getPrefix() + "://" + org.auraframework.impl.java.model.TestModel.class.getName();
        }

        @Override
        public String getDescriptorName() {
            return org.auraframework.impl.java.model.TestModel.class.getName();
        }

        @Override
        public boolean isParameterized() {
            return false;
        }

        @Override
        public String getNameParameters() {
            return null;
        }

        @Override
        public org.auraframework.def.DefDescriptor.DefType getDefType() {
            return DefType.MODEL;
        }

        @Override
        public ModelDef getDef() {
            return null;
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public int compareTo(DefDescriptor<?> other) {
            return DefDescriptorImpl.compare(this, other);
        }

        @Override
        public DefDescriptor<? extends Definition> getBundle() {
            return null;
        }
    };

    public JavaModelTest(String name) {
        super(name);
    }

    public void testSerializeMetadata() throws Exception {
        JavaModelDefFactory factory = new JavaModelDefFactory();
        ModelDef def = factory.getDef(descriptor);
        serializeAndGoldFile(def);
    }

    public void testSerializeData() throws Exception {
        JavaModelDefFactory factory = new JavaModelDefFactory(null);
        ModelDef def = factory.getDef(descriptor);
        Model model = def.newInstance();
        serializeAndGoldFile(model);
    }

    /**
     * Verify that class level annotation is required for a java model.
     *
     * @userStory a07B0000000FAmj
     */
    public void _testClassLevelAnnotationForJavaModel() throws Exception {
        DefDescriptor<ModelDef> javaModelDefDesc = DefDescriptorImpl.getInstance(
                "java://org.auraframework.impl.java.model.TestModel", ModelDef.class);
        assertNotNull(definitionService.getDefinition(javaModelDefDesc));

        DefDescriptor<ModelDef> javaModelWOAnnotationDefDesc = DefDescriptorImpl.getInstance(
                "java://org.auraframework.impl.java.model.TestModelWithoutAnnotation", ModelDef.class);
        try {
        	definitionService.getDefinition(javaModelWOAnnotationDefDesc);
            fail("Expected InvalidDefinitionException");
        } catch (Exception e) {
            checkExceptionStart(e, InvalidDefinitionException.class, "@Model annotation is required on all Models.",
                    javaModelWOAnnotationDefDesc.getName());
        }
    }

    /**
     * Test subclassing.
     */
    public void _testModelSubclass() throws Exception {
        DefDescriptor<ModelDef> javaModelDefDesc = DefDescriptorImpl.getInstance(
                "java://org.auraframework.impl.java.model.TestModelSubclass", ModelDef.class);
        ModelDef def = definitionService.getDefinition(javaModelDefDesc);
        assertNotNull(def);
        Model model = def.newInstance();
        ValueDef vd = def.getMemberByName("nextThing");

        PropertyReferenceImpl refNextThing = new PropertyReferenceImpl("nextThing", new Location("test", 0));
        assertNotNull("Unable to find value def for 'nextThing'", vd);
        assertEquals("nextThing", model.getValue(refNextThing));

        vd = def.getMemberByName("firstThing");
        PropertyReferenceImpl refFirstThing = new PropertyReferenceImpl("firstThing", new Location("test", 1));
        assertNotNull("Unable to find value def for 'firstThing'", vd);
        assertEquals("firstThingDefault", model.getValue(refFirstThing));
    }

    /**
     * Verify that nice exception is thrown if model accessor is void
     */
    public void _testModelMethodSignatures() throws Exception {
        String[] failModels = new String[] { "java://org.auraframework.impl.java.model.TestModelWithVoid",
        "java://org.auraframework.impl.java.model.TestModelWithStatic" };
        String[] failMessages = new String[] { "@AuraEnabled annotation found on void method getNothing",
        "@AuraEnabled annotation found on invalid method getStaticString" };

        for (int i = 0; i < failModels.length; i++) {
            try {
                definitionService.getDefinition(failModels[i], ModelDef.class);
                fail("Expected InvalidDefinitionException on model " + failModels[i]);
            } catch (Exception e) {
                checkExceptionStart(e, InvalidDefinitionException.class, failMessages[i], failModels[i]);
            }
        }
    }

    /**
     * Verify that nice exception is thrown if model def doesn't exist
     */
    public void _testModelNotFound() throws Exception {
        DefDescriptor<ComponentDef> dd = addSourceAutoCleanup(ComponentDef.class,
                "<aura:component model='java://goats'/>");
        try {
            Aura.getInstanceService().getInstance(dd);
            fail("Expected DefinitionNotFoundException");
        } catch (DefinitionNotFoundException e) {
        	assertTrue(e.getMessage().contains("No MODEL named java://goats found"));
        }
    }

    /**
     * Verify model can be accessed in system namespace
     */
    public void _testModelInSystemNamespace() throws Exception {
        String resourceSource = "<aura:component model='java://org.auraframework.impl.java.model.TestModel'>Hello World!</aura:component>";

        DefDescriptor<? extends Definition> dd = getAuraTestingUtil().addSourceAutoCleanup(ComponentDef.class, resourceSource,
                StringSourceLoader.DEFAULT_NAMESPACE + ":testComponent", true);

        try {
            Aura.getInstanceService().getInstance(dd);
        } catch (NoAccessException e) {
            fail("Not Expected NoAccessException");
        }
    }

    /**
     * Verify model can not be accessed in custom namespace
     */
    public void _testModelInCustomNamespace() throws Exception {
        String resourceSource = "<aura:component model='java://org.auraframework.impl.java.model.TestModel'>Hello World!</aura:component>";

        DefDescriptor<? extends Definition> dd = getAuraTestingUtil().addSourceAutoCleanup(ComponentDef.class, resourceSource,
                StringSourceLoader.DEFAULT_CUSTOM_NAMESPACE + ":testComponent", false);

        try {
            Aura.getInstanceService().getInstance(dd);
            fail("Expected NoAccessException");
        } catch (NoAccessException e) {
            String errorMessage = "Access to model 'org.auraframework.impl.java.model:TestModel' from namespace '"+StringSourceLoader.DEFAULT_CUSTOM_NAMESPACE+"' in '"+dd.getQualifiedName()+"(COMPONENT)' disallowed by MasterDefRegistry.assertAccess()";
            assertEquals(errorMessage, e.getMessage());
        }
    }

    /**
     * Verify that accessing a non-existing property on model throws Exception.
     */
    public void _testNonExistingPropertiesOnModel() throws Exception {
        DefDescriptor<ModelDef> javaModelDefDesc = DefDescriptorImpl.getInstance(
                "java://org.auraframework.impl.java.model.TestModel", ModelDef.class);
        ModelDef mDef = definitionService.getDefinition(javaModelDefDesc);
        assertNotNull(mDef);
        Model model = mDef.newInstance();
        try {
            model.getValue(new PropertyReferenceImpl("fooBar", new Location("test", 0)));
            fail("Model should not be able to getValue of a non existing property.");
        } catch (Exception e) {
            checkExceptionStart(e, AuraExecutionException.class, "TestModel: no such property: fooBar",
                    javaModelDefDesc.getName());
        }
        try {
            model.getValue(new PropertyReferenceImpl("firstThi", new Location("test", 0)));
            fail("Model.getValue() on partial matches of property names should not be successful.");
        } catch (Exception e) {
            checkExceptionStart(e, AuraExecutionException.class, "TestModel: no such property: firstThi",
                    javaModelDefDesc.getName());
        }
    }

    private void checkInvalidModel(Class<?> clazz, String message) {
        DefDescriptor<ModelDef> desc = DefDescriptorImpl.getInstance("java://" + clazz.getName(), ModelDef.class);
        
        try {
            //definitionService.getDefinition(desc);
            ModelDef modelDef = definitionService.getDefinition(desc);
            ((JavaModelDefImpl)modelDef).getJavaType().newInstance();
            fail("Expected exception");
        } catch (Exception e) {
            checkExceptionStart(e, InvalidDefinitionException.class, message, clazz.getCanonicalName());
        }
    }

    @Annotations.Model
    public static class ModelPrivateConstructor {
        private ModelPrivateConstructor() {
        }
    };

    public void _testPrivateConstructor() throws Exception {
        checkInvalidModel(ModelPrivateConstructor.class, "Default constructor is not public");
    }

    public void testProtectedConstructor() throws Exception {
    	DefDescriptor<ModelDef> javaModelDefDesc = DefDescriptorImpl.getInstance(
                "java://org.auraframework.impl.java.model.TestModelWithProtectedConstructor", ModelDef.class);
        try {
        	definitionService.getDefinition(javaModelDefDesc);
        } catch (Exception e) {
        	System.out.println(e.getClass()+","+e.getMessage());
        }
    }

    @Annotations.Model
    public static class ModelBadConstructor {
        public ModelBadConstructor(String value) {
        }
    };

    public void _testBadConstructor() throws Exception {
        checkInvalidModel(ModelBadConstructor.class, "No default constructor found");
    }
}
