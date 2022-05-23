#!/usr/bin/env python3
import os

import aws_cdk as cdk

from magento_platform.magento_platform_stack import MagentoPlatformStack
from magento_platform.magento_db_stack import MagentoDBStack
from magento_platform.magento_es_stack import MagentoElasticsearchStack
from magento_platform.magento_app_stack import MagentoAppStack



app = cdk.App()
infrastructureStack = MagentoPlatformStack(app, "MagentoPlatformStack")
databaseStack = MagentoDBStack(app, "MagentoDBStack", infrastructureStack.vpc, infrastructureStack.RDS_security_group)
elasticsearchStack = MagentoElasticsearchStack(app, "MagentoElasticsearchStack", infrastructureStack.vpc, infrastructureStack.OS_security_group)
applicationStack = MagentoAppStack(app, "MagentoAppStack", infrastructureStack.vpc, infrastructureStack.App_security_group, databaseStack.database_instance, elasticsearchStack.es_domain)


databaseStack.add_dependency(infrastructureStack)
elasticsearchStack.add_dependency(infrastructureStack)
applicationStack.add_dependency(databaseStack)
applicationStack.add_dependency(elasticsearchStack)
 
app.synth()
