#!/usr/bin/env python3
import os

import aws_cdk as cdk

from magento_platform.magento_platform_stack import MagentoPlatformStack

app = cdk.App()
infrastructureStack = MagentoPlatformStack(app, "MagentoPlatformStack")

app.synth()
