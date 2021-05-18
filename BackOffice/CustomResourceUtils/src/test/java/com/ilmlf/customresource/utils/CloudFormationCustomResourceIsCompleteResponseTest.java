package com.ilmlf.customresource.utils;

import org.junit.jupiter.api.Assertions;

class CloudFormationCustomResourceIsCompleteResponseTest {

    @org.junit.jupiter.api.Test
    void isCompleteResponseIsProperlyFormated()  {
        CloudFormationCustomResourceIsCompleteResponse response = new CloudFormationCustomResourceIsCompleteResponse();
        response.setIsComplete(true);

        String responseAsString = response.toString();

        Assertions.assertFalse(responseAsString.contains("isComplete"));
        Assertions.assertTrue(responseAsString.contains("IsComplete"));
    }
}