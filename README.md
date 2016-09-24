# DCSIM-LQN
DCSIM-LQN is a simulator for evaluating deployment policies in a multi-cloud

DCSIM-LQN is a simulator for evaluating deployment policies in a multi-cloud, derived from DCSIM (a project of Univ. of Western Ontario). DCSIM-LQN adds two features related to the use of LQN (Layered Queueing Network) models of cloud applications (see layeredqueues.org for more on LQNs):
... definition of the cloud applications by one or more LQNs. This provides a flexible architectural definition including request relationships between VMs, and classes of service by a VM.
... evaluation of the performance of a given deployment by the LQN Solver (lqns), a tool that incorporates approximations for resource interactions in complex layered systems. 
