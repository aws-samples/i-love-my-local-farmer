import aws_cdk as core
import aws_cdk.assertions as assertions

from magento_platform.magento_platform_stack import MagentoPlatformStack

# example tests. To run these tests, uncomment this file along with the example
# resource in magento_platform/magento_platform_stack.py
def test_sqs_queue_created():
    app = core.App()
    stack = MagentoPlatformStack(app, "magento-platform")
    template = assertions.Template.from_stack(stack)
