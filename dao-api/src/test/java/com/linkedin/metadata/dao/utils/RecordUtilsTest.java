package com.linkedin.metadata.dao.utils;

import com.linkedin.common.urn.Urn;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.StringArray;
import com.linkedin.metadata.dao.BaseLocalDAO;
import com.linkedin.metadata.dao.exception.ModelConversionException;
import com.linkedin.metadata.validator.InvalidSchemaException;
import com.linkedin.metadata.validator.ValidationUtils;
import com.linkedin.testing.AspectBar;
import com.linkedin.testing.AspectBaz;
import com.linkedin.testing.AspectFoo;
import com.linkedin.testing.AspectBarArray;
import com.linkedin.testing.AspectFooArray;
import com.linkedin.testing.AspectWithDefaultValue;
import com.linkedin.testing.EntityAspectUnion;
import com.linkedin.testing.EntityAspectUnionAlias;
import com.linkedin.testing.EntityAspectUnionComplex;
import com.linkedin.testing.EntitySnapshot;
import com.linkedin.testing.EntityValueArray;
import com.linkedin.testing.MixedRecord;
import com.linkedin.testing.PizzaInfo;
import com.linkedin.testing.RelationshipV2Bar;
import com.linkedin.testing.StringUnion;
import com.linkedin.testing.StringUnionArray;
import com.linkedin.testing.MapValueRecord;
import com.linkedin.testing.singleaspectentity.EntityValue;
import com.linkedin.testing.urn.BarUrn;
import com.linkedin.testing.urn.FooUrn;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import static com.linkedin.testing.TestUtils.*;
import static org.testng.Assert.*;


public class RecordUtilsTest {

  private static String loadJsonFromResource(String resourceName) throws IOException {
    return IOUtils.toString(ClassLoader.getSystemResourceAsStream(resourceName), StandardCharsets.UTF_8);
  }

  @Test
  public void testToJsonString() throws IOException {
    AspectFoo foo = new AspectFoo().setValue("foo");
    String expected =
        loadJsonFromResource("foo.json").replaceAll("\\s+", "").replaceAll("\\n", "").replaceAll("\\r", "");

    String actual = RecordUtils.toJsonString(foo);

    assertEquals(actual, expected);
  }

  @Test
  public void testToJsonStringWithDefault() throws IOException {
    AspectWithDefaultValue defaultValueAspect = new AspectWithDefaultValue().setNestedValueWithDefault(new MapValueRecord());
    String expected =
        loadJsonFromResource("defaultValueAspect.json").replaceAll("\\s+", "").replaceAll("\\n", "").replaceAll("\\r", "");
    BaseLocalDAO.validateAgainstSchemaAndFillinDefault(defaultValueAspect);
    String actual = RecordUtils.toJsonString(defaultValueAspect);

    assertEquals(actual, expected);
  }

  @Test
  public void testToRecordTemplate() throws IOException {
    AspectFoo expected = new AspectFoo().setValue("foo");
    String jsonString = loadJsonFromResource("foo.json");

    AspectFoo actual = RecordUtils.toRecordTemplate(AspectFoo.class, jsonString);

    assertEquals(actual, expected);

    RecordTemplate actual2 = RecordUtils.toRecordTemplate("com.linkedin.testing.AspectFoo", expected.data());

    assertEquals(actual2.getClass(), AspectFoo.class);
    assertEquals(actual2, expected);
  }

  @Test(expectedExceptions = ModelConversionException.class)
  public void testToRecordTemplateFromInvalidString() {
    RecordUtils.toRecordTemplate(AspectFoo.class, "invalid_json");
  }

  @Test
  public void testGetValidRecordDataSchemaField() {
    RecordDataSchema schema = ValidationUtils.getRecordSchema(AspectFoo.class);
    RecordDataSchema.Field expected = schema.getField("value");

    assertEquals(RecordUtils.getRecordDataSchemaField(new AspectFoo().setValue("foo"), "value"), expected);
  }

  @Test(expectedExceptions = InvalidSchemaException.class)
  public void testGetInvalidRecordDataSchemaField() {
    RecordUtils.getRecordDataSchemaField(new AspectFoo().setValue("foo"), "non-existing-field");
  }

  @Test
  public void testSetRecordTemplatePrimitiveField() {
    AspectBaz baz = new AspectBaz();

    RecordUtils.setRecordTemplatePrimitiveField(baz, "boolField", Boolean.FALSE);
    RecordUtils.setRecordTemplatePrimitiveField(baz, "stringField", "baz");
    RecordUtils.setRecordTemplatePrimitiveField(baz, "longField", Long.valueOf(1234L));

    assertFalse(baz.isBoolField());
    assertEquals(baz.getStringField(), "baz");
    assertEquals(baz.getLongField(), Long.valueOf(1234L));
  }

  @Test
  public void testSetRecordTemplateComplexField() throws IOException {
    AspectBaz baz = new AspectBaz();

    StringArray stringArray = new StringArray(Arrays.asList("1", "2", "3"));
    RecordUtils.setRecordTemplateComplexField(baz, "arrayField", stringArray);

    AspectFoo foo = new AspectFoo().setValue("foo");
    RecordUtils.setRecordTemplateComplexField(baz, "recordField", foo);

    assertEquals(baz.getArrayField(), stringArray);
    assertEquals(baz.getRecordField(), foo);
  }

  @Test
  public void testGetRecordTemplatePrimitiveField() throws IOException {
    AspectBaz baz = loadAspectBaz("baz.json");

    assertTrue(RecordUtils.getRecordTemplateField(baz, "boolField", Boolean.class));
    assertEquals(RecordUtils.getRecordTemplateField(baz, "stringField", String.class), "baz");
    assertEquals(RecordUtils.getRecordTemplateField(baz, "longField", Long.class), Long.valueOf(1234L));
  }

  @Test
  public void testGetRecordTemplateUrnField() {
    Urn urn = makeUrn(1);
    EntitySnapshot snapshot = new EntitySnapshot().setUrn(urn);

    assertEquals(RecordUtils.getRecordTemplateField(snapshot, "urn", Urn.class), urn);
  }

  @Test
  public void testGetRecordTemplateWrappedField() throws IOException {
    AspectBaz baz = loadAspectBaz("baz.json");

    StringArray stringArray = RecordUtils.getRecordTemplateWrappedField(baz, "arrayField", StringArray.class);

    assertEquals(stringArray.toArray(), new String[]{"1", "2", "3"});
  }

  @Test
  public void testGetSelectedRecordTemplateFromUnion() throws IOException {
    AspectBaz baz = new AspectBaz();
    baz.setUnionField(new AspectBaz.UnionField());
    baz.getUnionField().setAspectFoo(new AspectFoo().setValue("foo"));

    RecordTemplate selected = RecordUtils.getSelectedRecordTemplateFromUnion(baz.getUnionField());

    assertEquals(selected.getClass(), AspectFoo.class);
  }

  @Test
  public void testGetSelectedRecordTemplateFromUnionWithTypeRef() throws Exception {
    AspectBaz baz = new AspectBaz();
    baz.setUnionField(new AspectBaz.UnionField());
    baz.getUnionField().setTyperefPizzaInfo(new PizzaInfo().setMadeBy("bar"));

    RecordTemplate selected = RecordUtils.getSelectedRecordTemplateFromUnion(baz.getUnionField());

    assertEquals(selected.getClass(), PizzaInfo.class);
    assertEquals(selected.getClass().getMethod("getMadeBy").invoke(selected), "bar");
    assertEquals(selected, new PizzaInfo().setMadeBy("bar"));
  }

  @Test
  public void testSetSelectedRecordTemplateInUnion() throws IOException {
    AspectBaz baz = new AspectBaz();
    baz.setUnionField(new AspectBaz.UnionField());
    AspectFoo expected = new AspectFoo().setValue("foo");

    RecordUtils.setSelectedRecordTemplateInUnion(baz.getUnionField(), expected);

    assertEquals(baz.getUnionField().getAspectFoo(), expected);
  }

  @Test
  public void testGetValidMetadataSnapshotClassFromName() {
    Class<? extends RecordTemplate> actualClass =
        ModelUtils.getMetadataSnapshotClassFromName("com.linkedin.testing.EntitySnapshot");
    assertEquals(actualClass, EntitySnapshot.class);
  }

  @Test(expectedExceptions = InvalidSchemaException.class)
  public void testGetInvalidMetadataSnapshotClassFromName() {
    ModelUtils.getMetadataSnapshotClassFromName("com.linkedin.testing.AspectInvalid");
  }

  @Test
  public void testGetPathSpecAsArray() {
    String ps1 = "/test/path";
    String[] psArray1 = RecordUtils.getPathSpecAsArray(ps1);

    assertEquals(psArray1.length, 2);
    assertEquals(psArray1[0], "test");
    assertEquals(psArray1[1], "path");

    String ps2 = "/";
    String[] psArray2 = RecordUtils.getPathSpecAsArray(ps2);

    assertEquals(psArray2.length, 0);

    String ps3 = "/test/";
    String[] psArray3 = RecordUtils.getPathSpecAsArray(ps3);

    assertEquals(psArray3.length, 1);
    assertEquals(psArray3[0], "test");

    String ps4 = "/recordArray/*/value";
    String[] psArray4 = RecordUtils.getPathSpecAsArray(ps4);

    assertEquals(psArray4.length, 3);
    assertEquals(psArray4[0], "recordArray");
    assertEquals(psArray4[1], "*");
    assertEquals(psArray4[2], "value");
  }

  @Test
  public void testGetAspectFromString() {
    assertEquals(RecordUtils.getAspectFromString(AspectBar.class.getCanonicalName()), new AspectBar());
    assertThrows(RuntimeException.class,
        () -> RecordUtils.getAspectFromString(""));
  }

  @Test
  public void testGetAspectFromClass() {
    assertEquals(RecordUtils.getAspectFromClass(AspectBar.class), new AspectBar());
  }

  @Test
  public void testExtractAspectFromSingleAspectEntity() {
    String field1 = "foo";
    EntityValue value = new EntityValue();
    value.setValue(field1);

    AspectBar aspect = new AspectBar();
    aspect.setValue(field1);

    assertEquals(RecordUtils.extractAspectFromSingleAspectEntity(value, AspectBar.class), aspect);
  }

  @Test(description = "Test getFieldValue() when RecordTemplate has primitive fields")
  public void testGetFieldValuePrimitive() {
    // case 1: string field set, bool field isn't set, default field should return default value
    final MixedRecord mixedRecord1 = new MixedRecord().setValue("fooVal1");
    PathSpec ps1 = MixedRecord.fields().value();
    PathSpec ps2 = MixedRecord.fields().flag();
    PathSpec ps3 = MixedRecord.fields().defaultField();

    Optional<Object> o1 = RecordUtils.getFieldValue(mixedRecord1, ps1);
    Optional<Object> o2 = RecordUtils.getFieldValue(mixedRecord1, ps2);
    Optional<Object> o3 = RecordUtils.getFieldValue(mixedRecord1, ps3);
    assertEquals(o1.get(), "fooVal1");
    assertFalse(o2.isPresent());
    assertEquals(o3.get(), "defaultVal");
    assertEquals(ps1.toString(), "/value");
    assertEquals(ps2.toString(), "/flag");
    assertEquals(ps3.toString(), "/defaultField");

    // case 2: string and bool field both set
    final MixedRecord mixedRecord2 = new MixedRecord().setValue("fooVal2").setFlag(true);
    Object o4 = RecordUtils.getFieldValue(mixedRecord2, MixedRecord.fields().value()).get();
    Object o5 = RecordUtils.getFieldValue(mixedRecord2, MixedRecord.fields().flag()).get();
    assertEquals(o4, "fooVal2");
    assertEquals(o5, true);

    // case 3: similar to case1, just that pegasus path as string is used as input
    Object o6 = RecordUtils.getFieldValue(mixedRecord1, "/value");
    assertEquals(o6, o1);
  }

  @Test(description = "Test getFieldValue() when RecordTemplate has TypeRef field")
  public void testGetFieldValueTypeRef() {
    // case 1: Urn as the TypeRef
    FooUrn urn = makeFooUrn(1);
    final MixedRecord mixedRecord1 = new MixedRecord().setFooUrn(urn);
    PathSpec ps1 = MixedRecord.fields().fooUrn();
    Object o1 = RecordUtils.getFieldValue(mixedRecord1, ps1).get();
    assertEquals(o1, urn);
    assertEquals(ps1.toString(), "/fooUrn");

    // case 2: TypeRef defined in the same pdl
    final MixedRecord mixedRecord2 = new MixedRecord().setIntTypeRef(2);
    PathSpec ps2 = MixedRecord.fields().intTypeRef();
    Object o2 = RecordUtils.getFieldValue(mixedRecord2, ps2).get();
    assertEquals(o2, 2);
    assertEquals(ps2.toString(), "/intTypeRef");

    // case 3: TypeRef for Record field reference
    AspectFoo aspectFoo = new AspectFoo().setValue("fooVal");
    PathSpec ps3 = MixedRecord.fields().recordTypeRef().value();
    final MixedRecord mixedRecord3 = new MixedRecord().setRecordTypeRef(aspectFoo);
    Object o3 = RecordUtils.getFieldValue(mixedRecord3, ps3).get();
    assertEquals(o3, "fooVal");
    assertEquals(ps3.toString(), "/recordTypeRef/value");
  }

  @Test(description = "Test getFieldValue() when RecordTemplate has another field of Record type")
  public void testGetFieldValueRecordType() {
    // case 1: referencing a field inside a RecordTemplate, one level deep.
    AspectFoo foo1 = new AspectFoo().setValue("fooVal1");
    MixedRecord mixedRecord1 = new MixedRecord().setRecordField(foo1);
    PathSpec ps1f1 = MixedRecord.fields().recordField().value();
    PathSpec ps1f2 =
        MixedRecord.fields().nestedRecordField().foo().value(); // referencing a nullable record template field

    Optional<Object> o1f1 = RecordUtils.getFieldValue(mixedRecord1, ps1f1);
    Optional<Object> o1f2 = RecordUtils.getFieldValue(mixedRecord1, ps1f2);

    assertEquals(o1f1.get(), "fooVal1");
    assertEquals(ps1f1.toString(), "/recordField/value");
    assertFalse(o1f2.isPresent());
    assertEquals(ps1f2.toString(), "/nestedRecordField/foo/value");

    // case 2: referencing a field inside a RecordTemplate, two levels deep i.e. nested field
    AspectFoo foo2 = new AspectFoo().setValue("fooVal2");
    com.linkedin.testing.EntityValue entityValue = new com.linkedin.testing.EntityValue().setFoo(foo2);
    MixedRecord mixedRecord2 = new MixedRecord().setNestedRecordField(entityValue);
    PathSpec ps2 = MixedRecord.fields().nestedRecordField().foo().value();

    Object o2 = RecordUtils.getFieldValue(mixedRecord2, ps2).get();

    assertEquals(o2, "fooVal2");
    assertEquals(ps2.toString(), "/nestedRecordField/foo/value");
  }

  @Test(description = "Test getFieldValue() when RecordTemplate has field of type array")
  public void testGetFieldValueArray() {

    // case 1: array of strings
    final MixedRecord mixedRecord1 =
        new MixedRecord().setStringArray(new StringArray(Arrays.asList("val1", "val2", "val3", "val4")));

    PathSpec ps1 = MixedRecord.fields().stringArray();
    Object o1 = RecordUtils.getFieldValue(mixedRecord1, ps1).get();

    assertEquals(o1, new StringArray(Arrays.asList("val1", "val2", "val3", "val4")));
    assertEquals(ps1.toString(), "/stringArray");

    // case 2: wildcard on array of records
    AspectFoo aspectFoo1 = new AspectFoo().setValue("fooVal1");
    AspectFoo aspectFoo2 = new AspectFoo().setValue("fooVal2");
    AspectFoo aspectFoo3 = new AspectFoo().setValue("fooVal3");
    AspectFoo aspectFoo4 = new AspectFoo().setValue("fooVal4");
    final AspectFooArray aspectFooArray =
        new AspectFooArray(Arrays.asList(aspectFoo1, aspectFoo2, aspectFoo3, aspectFoo4));
    final MixedRecord mixedRecord2 = new MixedRecord().setRecordArray(aspectFooArray);

    PathSpec ps2 = MixedRecord.fields().recordArray().items().value();
    Object o2 = RecordUtils.getFieldValue(mixedRecord2, ps2).get();

    assertEquals(o2, new StringArray(Arrays.asList("fooVal1", "fooVal2", "fooVal3", "fooVal4")));
    assertEquals(ps2.toString(), "/recordArray/*/value");

    // case 3: array of records is empty
    final MixedRecord mixedRecord3 = new MixedRecord().setRecordArray(new AspectFooArray());
    Object o3 = RecordUtils.getFieldValue(mixedRecord3, MixedRecord.fields().recordArray().items().value()).get();
    assertEquals(o3, new StringArray());

    // case 4: referencing an index of array is not supported
    final MixedRecord mixedRecord4 = new MixedRecord().setRecordArray(aspectFooArray);

    assertThrows(UnsupportedOperationException.class,
        () -> RecordUtils.getFieldValue(mixedRecord4, "/recordArray/0/value"));

    // case 5: referencing nested field inside array of records, field being 2 levels deep
    AspectFoo f1 = new AspectFoo().setValue("val1");
    AspectFoo f2 = new AspectFoo().setValue("val2");
    com.linkedin.testing.EntityValue val1 = new com.linkedin.testing.EntityValue().setFoo(f1);
    com.linkedin.testing.EntityValue val2 = new com.linkedin.testing.EntityValue().setFoo(f2);
    EntityValueArray entityValues = new EntityValueArray(Arrays.asList(val1, val2));
    final MixedRecord mixedRecord5 = new MixedRecord().setNestedRecordArray(entityValues);

    PathSpec psFoo5 = MixedRecord.fields().nestedRecordArray().items().foo().value();
    PathSpec psBar5 = MixedRecord.fields().nestedRecordArray().items().bar().value();
    Optional<Object> oFoo5 = RecordUtils.getFieldValue(mixedRecord5, psFoo5);
    Optional<Object> oBar5 = RecordUtils.getFieldValue(mixedRecord5, psBar5);

    assertEquals(oFoo5.get(), new StringArray("val1", "val2"));
    assertEquals(psFoo5.toString(), "/nestedRecordArray/*/foo/value");
    assertEquals(oBar5.get(), new StringArray());
    assertEquals(psBar5.toString(), "/nestedRecordArray/*/bar/value");

    // case 6: optional field containing array of strings is not set
    final MixedRecord mixedRecord6 = new MixedRecord();
    PathSpec ps6 = MixedRecord.fields().stringArray();
    Optional<Object> o6 = RecordUtils.getFieldValue(mixedRecord6, ps6);
    assertFalse(o6.isPresent());

    // case 7: optional field containing array of records is not set
    final MixedRecord mixedRecord7 = new MixedRecord();
    PathSpec ps7 = MixedRecord.fields().recordArray().items().value();
    Optional<Object> o7 = RecordUtils.getFieldValue(mixedRecord7, ps7);
    assertFalse(o7.isPresent());
  }

  @Test(description = "Test getFieldValue() when RecordTemplate has field of type array of primitive unions")
  public void testGetFieldValueArrayOfPrimitiveUnions() {

    // case 1: array of unions of strings
    final MixedRecord mixedRecord1 =
        new MixedRecord().setUnionArray(new StringUnionArray(Arrays.asList(
            StringUnion.create("val1"),
            StringUnion.create("val2"),
            StringUnion.create("val3"),
            StringUnion.create("val4")
        )));

    PathSpec ps1 = MixedRecord.fields().unionArray();
    Object o1 = RecordUtils.getFieldValue(mixedRecord1, ps1).get();

    PathSpec ps2 = MixedRecord.fields().unionArray().items();
    Object o2 = RecordUtils.getFieldValue(mixedRecord1, ps2).get();

    assertEquals(o1, new StringUnionArray(Arrays.asList(
        StringUnion.create("val1"),
        StringUnion.create("val2"),
        StringUnion.create("val3"),
        StringUnion.create("val4")
    )));
    assertEquals(ps1.toString(), "/unionArray");

    assertEquals(o2, new StringUnionArray(Arrays.asList(
        StringUnion.create("val1"),
        StringUnion.create("val2"),
        StringUnion.create("val3"),
        StringUnion.create("val4")
    )));
    assertEquals(ps2.toString(), "/unionArray/*");
  }

  @Test
  public void testGetFieldValuePrimitiveUnion() {
    final MixedRecord mixedRecord1 = new MixedRecord().setPrimitiveUnion(StringUnion.create("val1"));

    PathSpec ps1 = MixedRecord.fields().primitiveUnion();
    Object o1 = RecordUtils.getFieldValue(mixedRecord1, ps1).get();

    assertEquals(o1, StringUnion.create("val1"));
    assertEquals(ps1.toString(), "/primitiveUnion");
  }

  @Test
  public void testGetFieldValueUnionOfRecords() {
    AspectFoo foo0 = new AspectFoo().setValue("foo0");
    AspectBar bar0 = new AspectBar().setValue("bar0");
    AspectBar bar1 = new AspectBar().setValue("bar1");
    AspectBaz baz = new AspectBaz().setArrayRecordsField(new AspectBarArray(Arrays.asList(bar0, bar1)));
    EntityAspectUnion union = EntityAspectUnion.create(foo0); // Union members are AspectFoo and AspectBar
    EntityAspectUnionComplex unionComplex = EntityAspectUnionComplex.create(baz);
    EntityAspectUnionAlias unionAlias = new EntityAspectUnionAlias();
    unionAlias.setFoo(foo0);
    unionAlias.setBar(bar0);
    final MixedRecord mixedRecord1 = new MixedRecord()
        .setRecordUnion(union)
        .setRecordUnionComplex(unionComplex)
        .setRecordUnionAlias(unionAlias);

    // union
    PathSpec ps1 = MixedRecord.fields().recordUnion();
    Object o1 = RecordUtils.getFieldValue(mixedRecord1, ps1).get();

    // union's AspectFoo
    PathSpec ps2 = MixedRecord.fields().recordUnion().AspectFoo();
    Object o2 = RecordUtils.getFieldValue(mixedRecord1, ps2).get();

    // union's AspectFoo's value
    PathSpec ps3 = MixedRecord.fields().recordUnion().AspectFoo().value();
    Object o3 = RecordUtils.getFieldValue(mixedRecord1, ps3).get();

    // union's AspectBar's value - this should return an empty Optional, and no error should be thrown.
    PathSpec ps4 = MixedRecord.fields().recordUnion().AspectBar().value();
    Optional<Object> o4 = RecordUtils.getFieldValue(mixedRecord1, ps4);

    // union with AspectBaz, which has an array field containing AspectFoo records. try to access the value field of those records.
    PathSpec ps5 = MixedRecord.fields().recordUnionComplex().AspectBaz().arrayRecordsField().items().value();
    Object o5 = RecordUtils.getFieldValue(mixedRecord1, ps5).get();

    // union alias
    PathSpec ps6 = MixedRecord.fields().recordUnionAlias();
    Object o6 = RecordUtils.getFieldValue(mixedRecord1, ps6).get();

    // union alias' member's value
    PathSpec ps7 = MixedRecord.fields().recordUnionAlias().Bar().value();
    Object o7 = RecordUtils.getFieldValue(mixedRecord1, ps7).get();

    assertEquals(o1, EntityAspectUnion.create(foo0));
    assertEquals(ps1.toString(), "/recordUnion");

    assertEquals(o2, new AspectFoo().setValue("foo0"));
    assertEquals(ps2.toString(), "/recordUnion/com.linkedin.testing.AspectFoo");

    assertEquals(o3, "foo0");
    assertEquals(ps3.toString(), "/recordUnion/com.linkedin.testing.AspectFoo/value");

    assertFalse(o4.isPresent());

    assertEquals(((List<String>) o5).get(0), "bar0");
    assertEquals(((List<String>) o5).get(1), "bar1");
    assertEquals(ps5.toString(), "/recordUnionComplex/com.linkedin.testing.AspectBaz/arrayRecordsField/*/value");

    assertEquals(o6, unionAlias);
    assertEquals(ps6.toString(), "/recordUnionAlias");

    assertEquals(o7, "bar0");
    assertEquals(ps7.toString(), "/recordUnionAlias/bar/value");
  }

  @Test
  public void testCapitalizeFirst() {
    String s = "field1";
    assertEquals(RecordUtils.capitalizeFirst(s), "Field1");

    s = "t";
    assertEquals(RecordUtils.capitalizeFirst(s), "T");

    s = "";
    assertEquals(RecordUtils.capitalizeFirst(s), "");
  }

  @Test
  public void testExtractFieldNameFromUnionField() {
    BarUrn barUrn = makeBarUrn(1);
    RelationshipV2Bar relationshipV2 = mockRelationshipV2Bar(barUrn);

    String destinationFieldName = RecordUtils.extractFieldNameFromUnionField(relationshipV2, "destination");
    assertEquals(destinationFieldName, "destinationBar");
  }

  @Test
  public void testExtractFieldValueFromUnionField() {
    BarUrn barUrn = makeBarUrn(1);
    RelationshipV2Bar relationshipV2 = mockRelationshipV2Bar(barUrn);

    String destinationFieldValue = RecordUtils.extractFieldValueFromUnionField(relationshipV2, "destination");
    assertEquals(destinationFieldValue, makeBarUrn(1).toString());
  }

  private AspectBaz loadAspectBaz(String resourceName) throws IOException {
    return RecordUtils.toRecordTemplate(AspectBaz.class,
        IOUtils.toString(ClassLoader.getSystemResourceAsStream(resourceName), StandardCharsets.UTF_8));
  }

  private RelationshipV2Bar mockRelationshipV2Bar(BarUrn barUrn) {
    RelationshipV2Bar.Destination destination = new RelationshipV2Bar.Destination();
    destination.setDestinationBar(barUrn);
    return new RelationshipV2Bar().setDestination(destination);
  }
}
