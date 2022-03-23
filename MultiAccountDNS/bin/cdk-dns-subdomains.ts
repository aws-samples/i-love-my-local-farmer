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

const { rootZoneDelegationRole, subZoneDelegationRoles } = new RootDomainStack(app, 'RootDomainStack', {
  description: 'Create hosted zones for each domain and update registered domain (uksb-1sqbr69tp)',
  env: root,
  subDomainEnvs: subEnvs,
  envWithRootAccess: subEnvs.prod,
});

const devStack = new SubDomainStack(app, 'DevSubDomainStack', {
  description: 'deploy dev app, create ssl certificates and update subdomain records (uksb-1sqbr69tp)',
  env: subEnvs.dev,
  zoneDelegationRole: subZoneDelegationRoles.dev,
});

const stagingStack = new SubDomainStack(app, 'StagingSubDomainStack', {
description: 'deploy staging app, create ssl certificates and update subdomain records (uksb-1sqbr69tp)',
  env: subEnvs.staging,
  zoneDelegationRole: subZoneDelegationRoles.staging,
});

const prodStack = new SubDomainStack(app, 'ProdSubDomainStack', {
  description: 'deploy prod app, create ssl certificates and update both root & sub domain records (uksb-1sqbr69tp)',
  env: subEnvs.prod,
  zoneDelegationRole: subZoneDelegationRoles.prod,
  rootZoneDelegationRole: rootZoneDelegationRole, // To make root domain localfarmer.com point to prod
});
