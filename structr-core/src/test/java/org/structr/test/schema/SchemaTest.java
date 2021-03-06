/**
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.test.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.SchemaView;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.Actions;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonReferenceProperty;
import org.structr.schema.json.JsonReferenceType;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonSchema.Cascade;
import org.structr.schema.json.JsonType;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 *
 *
 */
public class SchemaTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(SchemaTest.class.getName());

	@Test
	public void test00SimpleProperties() {

		cleanDatabaseAndSchema();

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			// a customer
			final JsonType customer = sourceSchema.addType("Customer");

			customer.addStringProperty("name", "public", "ui").setRequired(true).setUnique(true);
			customer.addStringProperty("street", "public", "ui");
			customer.addStringProperty("city", "public", "ui");
			customer.addDateProperty("birthday", "public", "ui");
			customer.addEnumProperty("status", "public", "ui").setEnums("active", "retired", "none").setDefaultValue("active");
			customer.addIntegerProperty("count", "public", "ui").setMinimum(1).setMaximum(10, true).setDefaultValue("5");
			customer.addNumberProperty("number", "public", "ui").setMinimum(2.0, true).setMaximum(5.0, true).setDefaultValue("3.0");
			customer.addLongProperty("loong", "public", "ui").setMinimum(20, true).setMaximum(50);
			customer.addBooleanProperty("isCustomer", "public", "ui");
			customer.addFunctionProperty("displayName", "public", "ui").setReadFunction("concat(this.name, '.', this.id)");
			customer.addStringProperty("description", "public", "ui").setContentType("text/plain").setFormat("multi-line");
			customer.addStringArrayProperty("stringArray", "public", "ui");
			customer.addIntegerArrayProperty("intArray", "public", "ui").setMinimum(0, true).setMaximum(100, true);
			customer.addLongArrayProperty("longArray", "public", "ui").setMinimum(1, true).setMaximum(101, true);
			customer.addDoubleArrayProperty("doubleArray", "public", "ui").setMinimum(2.0, true).setMaximum(102.0, true);
			customer.addBooleanArrayProperty("booleanArray", "public", "ui");

			final String schema = sourceSchema.toString();

			final Map<String, Object> map = new GsonBuilder().create().fromJson(schema, Map.class);

			mapPathValue(map, "definitions.Customer.type",                                          "object");
			mapPathValue(map, "definitions.Customer.required.0",                                    "name");
			mapPathValue(map, "definitions.Customer.properties.booleanArray.type",                   "array");
			mapPathValue(map, "definitions.Customer.properties.booleanArray.items.type",             "boolean");
			mapPathValue(map, "definitions.Customer.properties.city.unique",                         null);
			mapPathValue(map, "definitions.Customer.properties.count.type",                          "integer");
			mapPathValue(map, "definitions.Customer.properties.count.minimum",                       1.0);
			mapPathValue(map, "definitions.Customer.properties.count.maximum",                       10.0);
			mapPathValue(map, "definitions.Customer.properties.count.exclusiveMaximum",              true);
			mapPathValue(map, "definitions.Customer.properties.doubleArray.type",                   "array");
			mapPathValue(map, "definitions.Customer.properties.doubleArray.items.type",             "number");
			mapPathValue(map, "definitions.Customer.properties.doubleArray.items.exclusiveMaximum", true);
			mapPathValue(map, "definitions.Customer.properties.doubleArray.items.exclusiveMaximum", true);
			mapPathValue(map, "definitions.Customer.properties.doubleArray.items.maximum",          102.0);
			mapPathValue(map, "definitions.Customer.properties.doubleArray.items.minimum",          2.0);
			mapPathValue(map, "definitions.Customer.properties.number.type",                        "number");
			mapPathValue(map, "definitions.Customer.properties.number.minimum",                     2.0);
			mapPathValue(map, "definitions.Customer.properties.number.maximum",                     5.0);
			mapPathValue(map, "definitions.Customer.properties.number.exclusiveMinimum",            true);
			mapPathValue(map, "definitions.Customer.properties.number.exclusiveMaximum",            true);
			mapPathValue(map, "definitions.Customer.properties.longArray.type",                     "array");
			mapPathValue(map, "definitions.Customer.properties.longArray.items.type",               "long");
			mapPathValue(map, "definitions.Customer.properties.longArray.items.exclusiveMaximum",   true);
			mapPathValue(map, "definitions.Customer.properties.longArray.items.exclusiveMaximum",   true);
			mapPathValue(map, "definitions.Customer.properties.longArray.items.maximum",            101.0);
			mapPathValue(map, "definitions.Customer.properties.longArray.items.minimum",            1.0);
			mapPathValue(map, "definitions.Customer.properties.loong.type",                         "long");
			mapPathValue(map, "definitions.Customer.properties.loong.minimum",                      20.0);
			mapPathValue(map, "definitions.Customer.properties.loong.maximum",                      50.0);
			mapPathValue(map, "definitions.Customer.properties.loong.exclusiveMinimum",             true);
			mapPathValue(map, "definitions.Customer.properties.intArray.type",                      "array");
			mapPathValue(map, "definitions.Customer.properties.intArray.items.type",                "integer");
			mapPathValue(map, "definitions.Customer.properties.intArray.items.exclusiveMaximum",    true);
			mapPathValue(map, "definitions.Customer.properties.intArray.items.exclusiveMaximum",    true);
			mapPathValue(map, "definitions.Customer.properties.intArray.items.maximum",             100.0);
			mapPathValue(map, "definitions.Customer.properties.intArray.items.minimum",             0.0);
			mapPathValue(map, "definitions.Customer.properties.isCustomer.type",                    "boolean");
			mapPathValue(map, "definitions.Customer.properties.description.type",                   "string");
			mapPathValue(map, "definitions.Customer.properties.description.contentType",            "text/plain");
			mapPathValue(map, "definitions.Customer.properties.description.format",                 "multi-line");
			mapPathValue(map, "definitions.Customer.properties.displayName.type",                   "function");
			mapPathValue(map, "definitions.Customer.properties.displayName.readFunction",           "concat(this.name, '.', this.id)");
			mapPathValue(map, "definitions.Customer.properties.name.type",                          "string");
			mapPathValue(map, "definitions.Customer.properties.name.unique",                        true);
			mapPathValue(map, "definitions.Customer.properties.street.type",                        "string");
			mapPathValue(map, "definitions.Customer.properties.status.type",                        "string");
			mapPathValue(map, "definitions.Customer.properties.status.enum.0",                      "active");
			mapPathValue(map, "definitions.Customer.properties.status.enum.1",                      "none");
			mapPathValue(map, "definitions.Customer.properties.status.enum.2",                      "retired");
			mapPathValue(map, "definitions.Customer.properties.stringArray.type",                   "array");
			mapPathValue(map, "definitions.Customer.properties.stringArray.items.type",             "string");
			mapPathValue(map, "definitions.Customer.views.public.0",                                "birthday");
			mapPathValue(map, "definitions.Customer.views.public.1",                                "booleanArray");
			mapPathValue(map, "definitions.Customer.views.public.2",                                "city");
			mapPathValue(map, "definitions.Customer.views.public.3",                                "count");
			mapPathValue(map, "definitions.Customer.views.public.4",                                "description");
			mapPathValue(map, "definitions.Customer.views.public.5",                                "displayName");
			mapPathValue(map, "definitions.Customer.views.public.6",                                "doubleArray");
			mapPathValue(map, "definitions.Customer.views.public.7",                                "intArray");
			mapPathValue(map, "definitions.Customer.views.public.8",                                "isCustomer");
			mapPathValue(map, "definitions.Customer.views.public.9",                                "longArray");
			mapPathValue(map, "definitions.Customer.views.public.10",                               "loong");
			mapPathValue(map, "definitions.Customer.views.public.11",                               "name");
			mapPathValue(map, "definitions.Customer.views.public.12",                               "number");
			mapPathValue(map, "definitions.Customer.views.public.13",                               "status");
			mapPathValue(map, "definitions.Customer.views.public.14",                               "street");
			mapPathValue(map, "definitions.Customer.views.ui.0",                                    "birthday");
			mapPathValue(map, "definitions.Customer.views.ui.1",                                    "booleanArray");
			mapPathValue(map, "definitions.Customer.views.ui.2",                                    "city");
			mapPathValue(map, "definitions.Customer.views.ui.3",                                    "count");
			mapPathValue(map, "definitions.Customer.views.ui.4",                                    "description");
			mapPathValue(map, "definitions.Customer.views.ui.5",                                    "displayName");
			mapPathValue(map, "definitions.Customer.views.ui.6",                                    "doubleArray");
			mapPathValue(map, "definitions.Customer.views.ui.7",                                    "intArray");
			mapPathValue(map, "definitions.Customer.views.ui.8",                                    "isCustomer");
			mapPathValue(map, "definitions.Customer.views.ui.9",                                    "longArray");
			mapPathValue(map, "definitions.Customer.views.ui.10",                                   "loong");
			mapPathValue(map, "definitions.Customer.views.ui.11",                                   "name");
			mapPathValue(map, "definitions.Customer.views.ui.12",                                   "number");
			mapPathValue(map, "definitions.Customer.views.ui.13",                                   "status");
			mapPathValue(map, "definitions.Customer.views.ui.14",                                   "street");

			// advanced: test schema roundtrip
			compareSchemaRoundtrip(sourceSchema);

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test01Inheritance() {

		cleanDatabaseAndSchema();

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			final JsonType contact  = sourceSchema.addType("Contact").setExtends(sourceSchema.getType("Principal"));
			final JsonType customer = sourceSchema.addType("Customer").setExtends(contact);

			final String schema = sourceSchema.toString();

			final Map<String, Object> map = new GsonBuilder().create().fromJson(schema, Map.class);

			mapPathValue(map, "definitions.Contact.type",        "object");
			mapPathValue(map, "definitions.Contact.$extends.0",  "#/definitions/Principal");

			mapPathValue(map, "definitions.Customer.type",       "object");
			mapPathValue(map, "definitions.Customer.$extends.0", "#/definitions/Contact");


			// advanced: test schema roundtrip
			compareSchemaRoundtrip(sourceSchema);

		} catch (Exception t) {
			logger.warn("", t);
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test02SimpleSymmetricReferences() {

		cleanDatabaseAndSchema();

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project = sourceSchema.addType("Project");
			final JsonObjectType task    = sourceSchema.addType("Task");

			// create relation
			final JsonReferenceType rel = project.relate(task, "has", Cardinality.OneToMany, "project", "tasks");
			rel.setName("ProjectTasks");

			final String schema = sourceSchema.toString();

			// test map paths
			final Map<String, Object> map = new GsonBuilder().create().fromJson(schema, Map.class);

			mapPathValue(map, "definitions.Project.type",                        "object");
			mapPathValue(map, "definitions.Project.properties.tasks.$link",      "#/definitions/ProjectTasks");
			mapPathValue(map, "definitions.Project.properties.tasks.items.$ref", "#/definitions/Task");
			mapPathValue(map, "definitions.Project.properties.tasks.type",       "array");

			mapPathValue(map, "definitions.ProjectTasks.$source",                "#/definitions/Project");
			mapPathValue(map, "definitions.ProjectTasks.$target",                "#/definitions/Task");
			mapPathValue(map, "definitions.ProjectTasks.cardinality",            "OneToMany");
			mapPathValue(map, "definitions.ProjectTasks.rel",                    "has");
			mapPathValue(map, "definitions.ProjectTasks.sourceName",             "project");
			mapPathValue(map, "definitions.ProjectTasks.targetName",             "tasks");
			mapPathValue(map, "definitions.ProjectTasks.type",                   "object");

			mapPathValue(map, "definitions.Task.type",                           "object");
			mapPathValue(map, "definitions.Task.properties.project.$link",       "#/definitions/ProjectTasks");
			mapPathValue(map, "definitions.Task.properties.project.$ref",        "#/definitions/Project");
			mapPathValue(map, "definitions.Task.properties.project.type",        "object");

			// test
			compareSchemaRoundtrip(sourceSchema);

		} catch (FrameworkException | InvalidSchemaException |URISyntaxException ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test03SchemaBuilder() {

		cleanDatabaseAndSchema();

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final String instanceId       = app.getInstanceId();

			final JsonObjectType task = sourceSchema.addType("Task");
			final JsonProperty title  = task.addStringProperty("title", "public", "ui").setRequired(true);
			final JsonProperty desc   = task.addStringProperty("description", "public", "ui").setRequired(true);
			task.addDateProperty("description", "public", "ui").setDatePattern("dd.MM.yyyy").setRequired(true);

			// test function property
			task.addFunctionProperty("displayName", "public", "ui").setReadFunction("this.name");
			task.addFunctionProperty("javascript", "public", "ui").setReadFunction("{ var x = 'test'; return x; }").setContentType("application/x-structr-javascript");


			// a project
			final JsonObjectType project = sourceSchema.addType("Project");
			project.addStringProperty("name", "public", "ui").setRequired(true);

			final JsonReferenceType projectTasks = project.relate(task, "HAS", Cardinality.OneToMany, "project", "tasks");
			projectTasks.setCascadingCreate(Cascade.targetToSource);

			project.getViewPropertyNames("public").add("tasks");
			task.getViewPropertyNames("public").add("project");


			// test enums
			project.addEnumProperty("status", "ui").setEnums("active", "planned", "finished");


			// a worker
			final JsonObjectType worker = sourceSchema.addType("Worker");
			final JsonReferenceType workerTasks = worker.relate(task, "HAS", Cardinality.OneToMany, "worker", "tasks");
			workerTasks.setCascadingDelete(Cascade.sourceToTarget);


			// reference Worker -> Task
			final JsonReferenceProperty workerProperty = workerTasks.getSourceProperty();
			final JsonReferenceProperty tasksProperty  = workerTasks.getTargetProperty();
			tasksProperty.setName("renamedTasks");


			worker.addReferenceProperty("taskNames",  tasksProperty, "public", "ui").setProperties("name");
			worker.addReferenceProperty("taskInfos",  tasksProperty, "public", "ui").setProperties("id", "name");
			worker.addReferenceProperty("taskErrors", tasksProperty, "public", "ui").setProperties("id");


			task.addReferenceProperty("workerName",   workerProperty, "public", "ui").setProperties("name");
			task.addReferenceProperty("workerNotion", workerProperty, "public", "ui").setProperties("id");


			// test date properties..
			project.addDateProperty("startDate", "public", "ui");

			// methods
			project.addMethod("onCreate", "set(this, 'name', 'wurst')", "comment for wurst");



			// test URIs
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/#", sourceSchema.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task", task.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task/properties/title", title.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task/properties/description", desc.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Worker/properties/renamedTasks", tasksProperty.getId().toString());

			compareSchemaRoundtrip(sourceSchema);

		} catch (Exception ex) {

			ex.printStackTrace();
			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test04ManualSchemaRelatedPropertyNameCreation() {

		cleanDatabaseAndSchema();

		try {

			try (final Tx tx = app.tx()) {

				final SchemaNode source = app.create(SchemaNode.class, "Source");
				final SchemaNode target = app.create(SchemaNode.class, "Target");

				app.create(SchemaRelationshipNode.class,
					new NodeAttribute(SchemaRelationshipNode.relationshipType, "link"),
					new NodeAttribute(SchemaRelationshipNode.sourceNode, source),
					new NodeAttribute(SchemaRelationshipNode.targetNode, target),
					new NodeAttribute(SchemaRelationshipNode.sourceMultiplicity, "1"),
					new NodeAttribute(SchemaRelationshipNode.targetMultiplicity, "*")
				);

				tx.success();
			}

			checkSchemaString(StructrSchema.createFromDatabase(app).toString());

		} catch (FrameworkException | URISyntaxException t) {
			logger.warn("", t);
		}
	}

	@Test
	public void test05SchemaRelatedPropertyNameCreationWithPresets() {

		cleanDatabaseAndSchema();

		try {

			// create test case
			final JsonSchema schema     = StructrSchema.newInstance(URI.create(app.getInstanceId()));
			final JsonObjectType source = schema.addType("Source");
			final JsonObjectType target = schema.addType("Target");

			source.relate(target, "link", Relation.Cardinality.OneToMany, "sourceLink", "linkTargets");

			checkSchemaString(schema.toString());


		} catch (FrameworkException t) {
			logger.warn("", t);
		}

	}

	@Test
	public void test06SchemaRelatedPropertyNameCreationWithoutPresets() {

		cleanDatabaseAndSchema();

		try {

			// create test case
			final JsonSchema schema     = StructrSchema.newInstance(URI.create(app.getInstanceId()));
			final JsonObjectType source = schema.addType("Source");
			final JsonObjectType target = schema.addType("Target");

			source.relate(target, "link", Relation.Cardinality.OneToMany);

			checkSchemaString(schema.toString());

		} catch (FrameworkException t) {
			logger.warn("", t);
		}

	}

	@Test
	public void test00DeleteSchemaRelationshipInView() {

		cleanDatabaseAndSchema();

		Settings.CypherDebugLogging.setValue(true);

		SchemaRelationshipNode rel = null;

		try (final Tx tx = app.tx()) {

			// create source and target node
			final SchemaNode fooNode = app.create(SchemaNode.class, "Foo");
			final SchemaNode barNode = app.create(SchemaNode.class, "Bar");

			// create relationship
			rel = app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, fooNode),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, barNode),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "narf")
			);

			// create "public" view that contains the related property
			app.create(SchemaView.class,
				new NodeAttribute<>(SchemaView.name, "public"),
				new NodeAttribute<>(SchemaView.schemaNode, fooNode),
				new NodeAttribute<>(SchemaView.nonGraphProperties, "type, id, narfBars")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			app.delete(rel);
			tx.success();

		} catch (Throwable t) {

			// deletion of relationship should not fail
			t.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testJavaSchemaMethod() {

		cleanDatabaseAndSchema();

		final Class groupType = StructrApp.getConfiguration().getNodeEntityClass("Group");

		try (final Tx tx = app.tx()) {

			final SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName("Group").getFirst();

			assertNotNull("Schema node Group should exist", schemaNode);

			final StringBuilder source = new StringBuilder();

			source.append("final Set<String> test = new LinkedHashSet<>();\n");
			source.append("\t\ttest.add(\"one\");\n");
			source.append("\t\ttest.add(\"two\");\n");
			source.append("\t\ttest.add(\"three\");\n");
			source.append("\t\treturn test;\n\n");

			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.schemaNode, schemaNode),
				new NodeAttribute<>(SchemaMethod.name,       "testJavaMethod"),
				new NodeAttribute<>(SchemaMethod.source,     source.toString()),
				new NodeAttribute<>(SchemaMethod.codeType,   "java")
			);

			app.create(groupType, "test");

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Object result = Actions.execute(securityContext, null, "${first(find('Group')).testJavaMethod}", "test");

			assertTrue("Result should be of type Set", result instanceof Set);

			final Set<String> set = (Set)result;
			final String[] array  = set.toArray(new String[0]);

			assertEquals("Invalid Java schema method result", "one",   array[0]);
			assertEquals("Invalid Java schema method result", "two",   array[1]);
			assertEquals("Invalid Java schema method result", "three", array[2]);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testJavaSchemaMethodWithEmptySource() {

		cleanDatabaseAndSchema();

		try (final Tx tx = app.tx()) {

			final SchemaNode group = app.nodeQuery(SchemaNode.class).andName("Group").getFirst();

			assertNotNull("Schema node Group should exist", group);

			final String source = "";

			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.schemaNode, group),
				new NodeAttribute<>(SchemaMethod.name,       "testJavaMethod"),
				new NodeAttribute<>(SchemaMethod.source,     source),
				new NodeAttribute<>(SchemaMethod.codeType,   "java")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testViewInheritedFromInterface() {

		cleanDatabaseAndSchema();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			// make test type inherit from Favoritable (should add views)
			type.setImplements(URI.create("#/definitions/Favoritable"));

			// ----- interface Favoritable -----
			type.overrideMethod("setFavoriteContent",         false, "");
			type.overrideMethod("getFavoriteContent",         false, "return \"getFavoriteContent();\";");
			type.overrideMethod("getFavoriteContentType",     false, "return \"getContentType();\";");
			type.overrideMethod("getContext",                 false, "return \"getContext();\";");

			// add new type
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final Class testType        = StructrApp.getConfiguration().getNodeEntityClass("Test");
		final Set<String> views     = StructrApp.getConfiguration().getPropertyViewsForType(testType);

		assertTrue("Property view is not inherited correctly", views.contains("fav"));
	}

	@Test
	public void testBuiltinTypeFlag() {

		cleanDatabaseAndSchema();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			// add new type
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			// verify that all schema nodes have isBuiltinType set to true
			// except "Test"
			for (final SchemaNode schemaNode : app.nodeQuery(SchemaNode.class).getAsList()) {

				final String name  = schemaNode.getName();
				final boolean flag = schemaNode.getProperty(SchemaNode.isBuiltinType);

				if (name.equals("Test")) {

					assertFalse("Non-builtin type Test has isBuiltinType flag set", flag);

				} else {

					assertTrue("Builtin type " + name + " is missing isBuiltinType flag", flag);
				}
			}

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testNonGraphPropertyInView() {

		cleanDatabaseAndSchema();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			type.addViewProperty(PropertyView.Public, "createdBy");

			// add new type
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// check that createdBy is registered in the public view of type Test

			final Class test                   = StructrApp.getConfiguration().getNodeEntityClass("Test");
			final Set<PropertyKey> propertySet = StructrApp.getConfiguration().getPropertySet(test, PropertyView.Public);

			assertTrue("Non-graph property not registered correctly", propertySet.contains(GraphObject.createdBy));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testInheritedSchemaPropertyResolution() {

		cleanDatabaseAndSchema();

		// create "invalid" schema configuration
		try (final Tx tx = app.tx()) {

			final JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			type.setExtends(schema.getType("File"));

			type.addViewProperty(PropertyView.Public, "children");

			// add new type
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testModifiedPropertyValueAccessInScripting() {

		cleanDatabaseAndSchema();

		// schema setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			type.addStringProperty("desc");
			type.addStringProperty("nameBefore");
			type.addStringProperty("nameAfter");
			type.addStringProperty("descBefore");
			type.addStringProperty("descAfter");

			type.addMethod("onSave",
				"{"
					+ " var self = Structr.this;"
					+ " var mod = Structr.retrieve('modifications');"
					+ " self.nameBefore = mod.before.name;"
					+ " self.nameAfter  = mod.after.name;"
					+ " self.descBefore = mod.before.desc;"
					+ " self.descAfter  = mod.after.desc;"
				+ " }",
			"");

			// add new type
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		String uuid = null;

		final Class type = StructrApp.getConfiguration().getNodeEntityClass("Test");

		// create test object
		try (final Tx tx = app.tx()) {

			assertNotNull(type);

			final GraphObject obj = app.create(type, "test");

			uuid = obj.getUuid();

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test state before modification
		try (final Tx tx = app.tx()) {

			final GraphObject test = app.get(type, uuid);
			assertNotNull(test);

			assertNull("Invalid value before modification", test.getProperty("nameBefore"));
			assertNull("Invalid value before modification", test.getProperty("nameAfter"));
			assertNull("Invalid value before modification", test.getProperty("descBefore"));
			assertNull("Invalid value before modification", test.getProperty("descAfter"));

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// modify object
		try (final Tx tx = app.tx()) {

			final GraphObject test = app.get(type, uuid);
			assertNotNull(test);

			test.setProperty(StructrApp.key(type, "name"), "new test");
			test.setProperty(StructrApp.key(type, "desc"), "description");

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test state after modification
		try (final Tx tx = app.tx()) {

			final GraphObject test = app.get(type, uuid);
			assertNotNull(test);

			assertEquals("Invalid value after modification", "test",        test.getProperty("nameBefore"));
			assertEquals("Invalid value after modification", "new test",    test.getProperty("nameAfter"));
			assertNull("Invalid value after modification",                  test.getProperty("descBefore"));
			assertEquals("Invalid value after modification", "description", test.getProperty("descAfter"));

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	// ----- private methods -----
	private void checkSchemaString(final String source) {

		final Gson gson = new GsonBuilder().create();

		final Map<String, Object> map  = gson.fromJson(source, Map.class);
		assertNotNull("Invalid schema serialization", map);

		final Map<String, Object> defs = (Map)map.get("definitions");
		assertNotNull("Invalid schema serialization", defs);

		final Map<String, Object> src  = (Map)defs.get("Source");
		assertNotNull("Invalid schema serialization", src);

		final Map<String, Object> srcp = (Map)src.get("properties");
		assertNotNull("Invalid schema serialization", srcp);

		final Map<String, Object> tgt  = (Map)defs.get("Target");
		assertNotNull("Invalid schema serialization", tgt);

		final Map<String, Object> tgtp = (Map)tgt.get("properties");
		assertNotNull("Invalid schema serialization", tgtp);

		final Map<String, Object> lnk  = (Map)defs.get("SourcelinkTarget");
		assertNotNull("Invalid schema serialization", lnk);

		// check related property names
		assertTrue("Invalid schema serialization result", srcp.containsKey("linkTargets"));
		assertTrue("Invalid schema serialization result", tgtp.containsKey("sourceLink"));
		assertEquals("Invalid schema serialization result", "sourceLink", lnk.get("sourceName"));
		assertEquals("Invalid schema serialization result", "linkTargets", lnk.get("targetName"));
	}

	private void mapPathValue(final Map<String, Object> map, final String mapPath, final Object value) {

		final String[] parts = mapPath.split("[\\.]+");
		Object current       = map;

		for (int i=0; i<parts.length; i++) {

			final String part = parts[i];
			if (StringUtils.isNumeric(part)) {

				int index = Integer.valueOf(part);
				if (current instanceof List) {

					current = ((List)current).get(index);
				}

			} else {

				if (current instanceof Map) {

					current = ((Map)current).get(part);
				}
			}
		}

		assertEquals("Invalid map path result for " + mapPath, value, current);
	}

	private void compareSchemaRoundtrip(final JsonSchema sourceSchema) throws FrameworkException, InvalidSchemaException, URISyntaxException {

		final String source           = sourceSchema.toString();
		final JsonSchema targetSchema = StructrSchema.createFromSource(sourceSchema.toString());
		final String target           = targetSchema.toString();

		assertEquals("Invalid schema (de)serialization roundtrip result", source, target);

		StructrSchema.replaceDatabaseSchema(app, targetSchema);

		final JsonSchema replacedSchema = StructrSchema.createFromDatabase(app);
		final String replaced = replacedSchema.toString();

		assertEquals("Invalid schema replacement result", source, replaced);
	}
}
