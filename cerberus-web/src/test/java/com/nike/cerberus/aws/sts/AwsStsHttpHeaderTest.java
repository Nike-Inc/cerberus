package com.nike.cerberus.aws.sts;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class AwsStsHttpHeaderTest {

    @Test
    public void test_getRegion_returns_region_as_expected() {
        AwsStsHttpHeader header = new AwsStsHttpHeader(
                "20180904T205115Z",
                "FQoGZXIvYXdzEFYaDEYceadsfLKJLKlkj908098oB/rJIdxdo57fx3Ef2wW8WhFbSpLGg3hwNqhuepdkf/c0F7OXJutqM2yjgnZCiO7SPAdnMSJhoEgH7SJlkPaPfiRzZAf0yxxD6e4z0VJU74uQfbgfZpn5RL+JyDpgoYkUrjuyL8zRB1knGSOCi32Q75+asdfasd+7bWxMyJIKEb/HF2Le8xM/9F4WRqa5P0+asdfasdfasdf+MGlDlNG0KTzg1JT6QXf95ozWR5bBFSz5DbrFhXhMegMQ7+7Kvx+asdfasdl.jlkj++5NpRRlE54cct7+aG3HQskow9y73AU=",
                "AWS4-HMAC-SHA256 Credential=ASIA5S2FQS2GYQLK5FFF/20180904/us-west-2/sts/aws4_request, SignedHeaders=host;x-amz-date, Signature=ddb9417d2b9bfe6f8b03e31a8f5d8ab98e0f4alkj12312098asdf"
        );

        assertEquals("us-west-2", header.getRegion());

        header = new AwsStsHttpHeader(
                "20180904T205115Z",
                "FQoGZXIvYXdzEFYaDEYceadsfLKJLKlkj908098oB/rJIdxdo57fx3Ef2wW8WhFbSpLGg3hwNqhuepdkf/c0F7OXJutqM2yjgnZCiO7SPAdnMSJhoEgH7SJlkPaPfiRzZAf0yxxD6e4z0VJU74uQfbgfZpn5RL+JyDpgoYkUrjuyL8zRB1knGSOCi32Q75+asdfasd+7bWxMyJIKEb/HF2Le8xM/9F4WRqa5P0+asdfasdfasdf+MGlDlNG0KTzg1JT6QXf95ozWR5bBFSz5DbrFhXhMegMQ7+7Kvx+asdfasdl.jlkj++5NpRRlE54cct7+aG3HQskow9y73AU=",
                "AWS4-HMAC-SHA256 Credential=ASIA5S2FQS2GYQLK5FFF/20180904/us-east-1/sts/aws4_request, SignedHeaders=host;x-amz-date, Signature=ddb9417d2b9bfe6f8b03e31a8f5d8ab98e0f4alkj12312098asdf"
        );

        assertEquals("us-east-1", header.getRegion());
    }

}
