package com.nike.cerberus.auth.connector.onelogin;

import com.google.common.collect.Lists;
import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class OneLoginPojoTest {

  @Test
  public void test_pojo_structure_and_behavior() {

    List<Class> classes =
        Lists.newArrayList(
            CreateSessionLoginTokenRequest.class,
            CreateSessionLoginTokenResponse.class,
            GenerateTokenRequest.class,
            GenerateTokenResponse.class,
            GenerateTokenResponseData.class,
            GetUserResponse.class,
            MfaDevice.class,
            ResponseStatus.class,
            SessionLoginTokenData.class,
            SessionUser.class,
            UserData.class,
            VerifyFactorRequest.class,
            VerifyFactorResponse.class);

    List<PojoClass> pojoClasses =
        classes.stream().map(PojoClassFactory::getPojoClass).collect(Collectors.toList());

    Validator validator =
        ValidatorBuilder.create()
            .with(new GetterMustExistRule())
            .with(new SetterMustExistRule())
            .with(new SetterTester())
            .with(new GetterTester())
            .build();

    validator.validate(pojoClasses);
  }
}
