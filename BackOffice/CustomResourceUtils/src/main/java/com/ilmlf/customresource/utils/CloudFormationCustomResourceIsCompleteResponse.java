package com.ilmlf.customresource.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
/**
 * Properties that we include in the JSON response we send back to
 * CloudFormation. CloudFormation expects this response to be sent to a
 * presigned URL included in the request -- it doesn't look at the Lambda
 * response.
 *
 * @see <a href="https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/crpg-ref-responses.html">Custom resource response objects</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudFormationCustomResourceIsCompleteResponse {
    /**
     * Must be either "SUCCESS" or "FAILED".
     */
    private Boolean IsComplete;
}