#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { IlmlfE2ETestStack } from '../lib/ilmlf-e2e-test-stack';

const app = new cdk.App();
new IlmlfE2ETestStack(app, 'IlmlfE2ETestStack', {
  env: { region: 'us-west-2' },
});
