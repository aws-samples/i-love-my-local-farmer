# BaseInfra

Here is our CDK deployment for our work-from-home base infrastructure. It is the 'base infrastructure' because it is a prerequisite step to the client VPN set up. This is because it creates the VPC in which the Client VPN is later created.

This CDK app deploys an AWS VPC, and the Site-to-Site VPN connection between it and our on-premise environment.
