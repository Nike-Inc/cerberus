package com.nike.cerberus.record;

import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class RecordPojoTest {

    @Test
    public void test_pojo_structure_and_behavior() {

        List<PojoClass> pojoClasses = PojoClassFactory.getPojoClasses("com.nike.cerberus.record");

        Assert.assertEquals(12, pojoClasses.size());

        Validator validator = ValidatorBuilder.create()
                .with(new GetterMustExistRule())
                .with(new SetterMustExistRule())
                .with(new SetterTester())
                .with(new GetterTester())
                .build();

        validator.validate(pojoClasses);
    }

}
