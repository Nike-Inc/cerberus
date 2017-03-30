package com.nike.cerberus.auth.connector;

import com.google.common.collect.Lists;
import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class AuthConnectorPojoTest {

    @Test
    public void test_pojo_structure_and_behavior() {

        List<Class> classes = Lists.newArrayList(
                AuthData.class,
                AuthMfaDevice.class,
                AuthResponse.class
        );

        List<PojoClass> pojoClasses = classes.stream().map(PojoClassFactory::getPojoClass).collect(Collectors.toList());

        Validator validator = ValidatorBuilder.create()
                .with(new GetterMustExistRule())
                .with(new SetterMustExistRule())
                .with(new SetterTester())
                .with(new GetterTester())
                .build();

        validator.validate(pojoClasses);
    }

}
