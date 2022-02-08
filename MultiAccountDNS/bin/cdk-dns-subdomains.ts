#!/usr/bin/env node
import 'source-map-support/register';
import { App } from 'aws-cdk-lib';
import { RootDomainStack } from '../lib/rootDomain/RootDomainStack';
import { SubDomainStack } from '../lib/subDomain/SubDomainStack';

export type DomainEnv = {
  domain: string;
  account: string;
  region: string;
};

const app = new App();

const envs: { [env: string]: DomainEnv } = app.node.tryGetContext('environments');
const { root, ...subEnvs } = envs;

const { rootZoneDelegationRole, subZoneDelegationRoles } = new RootDomainStack(app, 'RootDomainStack5', {
  env: root,
  subDomainEnvs: subEnvs,
  envWithRootAccess: subEnvs.dev,
});

const devStack = new SubDomainStack(app, 'DevSubDomainStack', {
  env: subEnvs.dev,
  zoneDelegationRole: subZoneDelegationRoles.dev,
  rootZoneDelegationRole: rootZoneDelegationRole, // To make root domain localfarmer.com point to prod
});

const stagingStack = new SubDomainStack(app, 'StagingSubDomainStack', {
  env: subEnvs.staging,
  zoneDelegationRole: subZoneDelegationRoles.staging,
});

const prodStack = new SubDomainStack(app, 'ProdSubDomainStack', {
  env: subEnvs.prod,
  zoneDelegationRole: subZoneDelegationRoles.prod,
});
